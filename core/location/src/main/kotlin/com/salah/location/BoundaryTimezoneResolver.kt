package com.salah.location

import android.content.Context
import java.io.DataInputStream
import java.io.InputStream
import java.time.ZoneId
import java.util.zip.GZIPInputStream
import kotlin.math.cos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Land timezone boundaries. Overlap with a different time zone requires manual selection. */
internal class BoundaryTimezoneResolver(open: () -> InputStream) : TimezoneResolver {
    constructor(context: Context) : this({ context.assets.open("location/timezones.bin.gzip") })

    private val polygons by lazy {
        DataInputStream(GZIPInputStream(open()).buffered()).use { input ->
            check(input.readInt() == 2) { "Unsupported timezone dataset format" }
            List(input.readInt()) {
                Polygon(input.readUTF(), List(input.readInt()) {
                    val coordinates = IntArray(input.readInt() * 2)
                    for (index in coordinates.indices) {
                        coordinates[index] = input.readDelta() + if (index >= 2) coordinates[index - 2] else 0
                    }
                    Ring(coordinates)
                })
            }
        }
    }

    override suspend fun resolve(fix: LocationFix): ZoneId? = withContext(Dispatchers.IO) {
        val p = fix.coordinates
        val y = p.latitude * SCALE
        val x = p.longitude * SCALE
        val radius = maxOf(fix.accuracy.meters?.toDouble() ?: 0.0,
            if (fix.accuracy.approximate) 3_000.0 else if (fix.accuracy.meters == null) 1_000.0 else 0.0) + 25
        val latitudeRadius = radius * SCALE / 110_000
        val longitudeRadius = latitudeRadius / cos(Math.toRadians(p.latitude)).coerceAtLeast(0.001)
        if (x - longitudeRadius <= -180 * SCALE || x + longitudeRadius >= 180 * SCALE) return@withContext null

        var match: String? = null
        val nearby = mutableSetOf<String>()
        for (polygon in polygons) {
            ensureActive()
            if (!polygon.rings.first().bounds(x, y, longitudeRadius, latitudeRadius)) continue
            if (polygon.rings.any { it.nearBoundary(x, y, longitudeRadius, latitudeRadius) }) nearby += polygon.name
            if (polygon.rings.first().contains(x, y) && polygon.rings.drop(1).none { it.contains(x, y) }) {
                if (match != null && match != polygon.name) return@withContext null
                match = polygon.name
            }
        }
        match?.takeIf { name -> name in ZoneId.getAvailableZoneIds() && nearby.all { it == name } }?.let(ZoneId::of)
    }

    private fun DataInputStream.readDelta(): Int {
        var value = 0
        for (shift in 0..28 step 7) {
            val byte = readUnsignedByte()
            value = value or ((byte and 127) shl shift)
            if (byte < 128) return (value ushr 1) xor -(value and 1)
        }
        throw java.io.IOException("Invalid timezone coordinate encoding")
    }

    private class Polygon(val name: String, val rings: List<Ring>)

    private class Ring(private val vertices: IntArray) {
        private val west: Int
        private val east: Int
        private val south: Int
        private val north: Int

        init {
            val indices = vertices.indices step 2
            west = indices.minOf { vertices[it] }
            east = indices.maxOf { vertices[it] }
            south = indices.minOf { vertices[it + 1] }
            north = indices.maxOf { vertices[it + 1] }
        }

        fun bounds(x: Double, y: Double, dx: Double, dy: Double) =
            x + dx >= west && x - dx <= east && y + dy >= south && y - dy <= north

        fun contains(x: Double, y: Double): Boolean {
            if (!bounds(x, y, 0.0, 0.0)) return false
            var inside = false
            var previous = vertices.size - 2
            for (index in vertices.indices step 2) {
                val x1 = vertices[previous].toDouble()
                val y1 = vertices[previous + 1].toDouble()
                val x2 = vertices[index].toDouble()
                val y2 = vertices[index + 1].toDouble()
                if ((y1 > y) != (y2 > y) && x < (x2 - x1) * (y - y1) / (y2 - y1) + x1) inside = !inside
                previous = index
            }
            return inside
        }

        fun nearBoundary(x: Double, y: Double, dx: Double, dy: Double): Boolean {
            if (!bounds(x, y, dx, dy)) return false
            var previous = vertices.size - 2
            for (index in vertices.indices step 2) {
                val x1 = (vertices[previous] - x) / dx
                val y1 = (vertices[previous + 1] - y) / dy
                val x2 = (vertices[index] - x) / dx
                val y2 = (vertices[index + 1] - y) / dy
                previous = index
                if ((x1 > 1 && x2 > 1) || (x1 < -1 && x2 < -1) ||
                    (y1 > 1 && y2 > 1) || (y1 < -1 && y2 < -1)) continue
                val vx = x2 - x1
                val vy = y2 - y1
                val span = vx * vx + vy * vy
                val fraction = if (span == 0.0) 0.0 else (-(x1 * vx + y1 * vy) / span).coerceIn(0.0, 1.0)
                val distanceX = x1 + fraction * vx
                val distanceY = y1 + fraction * vy
                if (distanceX * distanceX + distanceY * distanceY <= 1) return true
            }
            return false
        }
    }

    private companion object { const val SCALE = 100_000.0 }
}
