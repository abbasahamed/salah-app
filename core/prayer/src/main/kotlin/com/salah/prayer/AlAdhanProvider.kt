package com.salah.prayer

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class AlAdhanProvider(
    private val baseUrl: HttpUrl = "https://api.aladhan.com/v1/".toHttpUrl(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .followRedirects(false)
        .retryOnConnectionFailure(false)
        .build(),
) : OnlinePrayerProvider {
    private val adapter = Moshi.Builder().build().adapter(AlAdhanResponse::class.java)

    override suspend fun fetch(request: PrayerCalculationRequest): DailyPrayerSchedule =
        suspendCancellableCoroutine { continuation ->
            val location = request.location
            val date = request.date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT))
            val url = baseUrl.newBuilder()
                .addPathSegment("timings").addPathSegment(date)
                .addQueryParameter("latitude", location.latitude.toString())
                .addQueryParameter("longitude", location.longitude.toString())
                .addQueryParameter("method", request.method.alAdhanId.toString())
                .addQueryParameter("school", request.asr.alAdhanId.toString())
                .addQueryParameter("timezonestring", location.zone.id)
                .addQueryParameter("latitudeAdjustmentMethod", "3")
                .addQueryParameter("iso8601", "true")
                .build()
            val call = client.newCall(Request.Builder().url(url).build())
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use {
                            if (!it.isSuccessful) throw IOException("AlAdhan HTTP ${it.code}")
                            val body = it.body ?: throw IOException("Missing AlAdhan response")
                            val source = body.source()
                            if (source.request(65_537)) throw IOException("Oversized AlAdhan response")
                            val payload = adapter.fromJson(source) ?: throw IOException("Empty AlAdhan response")
                            validate(request, date, payload)
                        }
                    }
                    continuation.resumeWith(result)
                }
            })
        }

    private fun validate(
        request: PrayerCalculationRequest,
        date: String,
        response: AlAdhanResponse,
    ): DailyPrayerSchedule {
        require(response.code == 200 && response.status == "OK")
        val data = response.data
        val location = request.location
        require(data.date.gregorian.date == date)
        require(data.meta.timezone == location.zone.id)
        require(data.meta.latitude == location.latitude && data.meta.longitude == location.longitude)
        require(data.meta.method.id == request.method.alAdhanId)
        require(data.meta.school == request.asr.name && data.meta.latitudeAdjustmentMethod == "ANGLE_BASED")
        require(PrayerName.entries.all { data.meta.offset[it.apiName] == 0 })
        val raw = PrayerName.entries.map { name ->
            val time = OffsetDateTime.parse(data.timings.getValue(name.apiName))
            val instant = time.toInstant()
            require(location.zone.rules.getOffset(instant) == time.offset)
            instant
        }
        return DailyPrayerSchedule(request, raw)
    }
}

@JsonClass(generateAdapter = true)
internal data class AlAdhanResponse(val code: Int, val status: String, val data: AlAdhanData)
@JsonClass(generateAdapter = true)
internal data class AlAdhanData(
    val timings: Map<String, String>,
    val date: AlAdhanDate,
    val meta: AlAdhanMeta,
)
@JsonClass(generateAdapter = true)
internal data class AlAdhanDate(val gregorian: AlAdhanGregorian)
@JsonClass(generateAdapter = true)
internal data class AlAdhanGregorian(val date: String)
@JsonClass(generateAdapter = true)
internal data class AlAdhanMeta(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val method: AlAdhanMethod,
    val school: String,
    val latitudeAdjustmentMethod: String,
    val offset: Map<String, Int>,
)
@JsonClass(generateAdapter = true)
internal data class AlAdhanMethod(val id: Int)
