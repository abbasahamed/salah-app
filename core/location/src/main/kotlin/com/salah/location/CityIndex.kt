package com.salah.location

import android.content.Context
import java.io.InputStream
import java.text.Normalizer
import java.time.ZoneId
import java.util.Locale
import java.util.zip.GZIPInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal fun interface CitySearch {
    suspend fun search(query: String): List<LocationSearchResult>
}

internal class CityIndex(open: () -> InputStream) : CitySearch {
    constructor(context: Context) : this({ context.assets.open("location/cities.tsv.gzip") })

    private data class City(val result: LocationSearchResult, val aliases: List<String>, val population: Long)

    private val cities by lazy {
        GZIPInputStream(open()).bufferedReader().useLines { lines ->
            val zones = ZoneId.getAvailableZoneIds().associateWith(ZoneId::of)
            lines.mapNotNull { line ->
                val f = line.split('\t')
                val zone = zones[f[7]] ?: return@mapNotNull null
                val names = PlaceNames(f[1], f[3].ifBlank { null }, f[4].ifBlank { null })
                City(
                    LocationSearchResult(f[0], names, LocationCoordinates(f[5].toDouble(), f[6].toDouble()), zone),
                    f[2].split(','),
                    f[8].toLongOrNull() ?: 0,
                )
            }.toList()
        }
    }

    override suspend fun search(query: String): List<LocationSearchResult> = withContext(Dispatchers.IO) {
        val term = normalize(query)
        if (term.length < 2) return@withContext emptyList()
        cities.asSequence().mapNotNull { city ->
            ensureActive()
            val rank = when {
                city.aliases.firstOrNull() == term -> 0
                term in city.aliases -> 1
                city.aliases.firstOrNull()?.startsWith(term) == true -> 2
                else -> return@mapNotNull null
            }
            rank to city
        }.sortedWith(compareBy<Pair<Int, City>> { it.first }.thenByDescending { it.second.population })
            .take(12).map { it.second.result }.toList()
    }

    private fun normalize(value: String) =
        Normalizer.normalize(value.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(DIACRITICS, "")

    private companion object {
        val DIACRITICS = Regex("\\p{M}+")
    }
}
