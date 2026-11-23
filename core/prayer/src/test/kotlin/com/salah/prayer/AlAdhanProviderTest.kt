package com.salah.prayer

import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AlAdhanProviderTest {
    @get:Rule val server = MockWebServer()
    private val provider by lazy {
        AlAdhanProvider(server.url("/v1/"), OkHttpClient.Builder()
            .callTimeout(1, TimeUnit.SECONDS).retryOnConnectionFailure(false).build())
    }

    @Test fun validResponseAndParameters() = runBlocking {
        enqueue()
        val schedule = provider.fetch(fixtureRequest.copy(asr = AsrJuristicMethod.HANAFI))
        assertEquals(Instant.parse("2026-11-20T23:27:00Z"), schedule.rawTimings.first())
        val url = server.takeRequest().requestUrl!!
        assertEquals("/v1/timings/21-11-2026", url.encodedPath)
        assertEquals("1", url.queryParameter("method"))
        assertEquals("1", url.queryParameter("school"))
        assertEquals("Asia/Kolkata", url.queryParameter("timezonestring"))
        assertEquals("3", url.queryParameter("latitudeAdjustmentMethod"))
        assertEquals("true", url.queryParameter("iso8601"))
    }

    @Test fun httpFailure() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        assertTrue(runCatching { provider.fetch(fixtureRequest) }.exceptionOrNull() is IOException)
    }

    @Test fun boundedTimeout() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertTrue(runCatching { provider.fetch(fixtureRequest) }.exceptionOrNull() is IOException)
    }

    @Test fun malformedJson() = rejects("{")
    @Test fun missingPrayer() = rejects(valid.replace("\"Sunrise\":\"2026-11-21T06:11:00+05:30\",", ""))
    @Test fun invalidDate() = rejects(valid.replace("21-11-2026", "20-11-2026"))
    @Test fun invalidZone() = rejects(valid.replace("Asia/Kolkata", "Europe/London"))
    @Test fun invalidOffset() = rejects(valid.replace("+05:30", "+05:00"))
    @Test fun wrongMethod() = rejects(valid.replace("\"id\":1", "\"id\":3"))

    private fun rejects(body: String) = runBlocking {
        enqueue(body)
        assertTrue(runCatching { provider.fetch(fixtureRequest.copy(asr = AsrJuristicMethod.HANAFI)) }.isFailure)
    }

    private fun enqueue(body: String = valid) { server.enqueue(MockResponse().setBody(body)) }

    private val valid = """
        {"code":200,"status":"OK","data":{
          "timings":{"Fajr":"2026-11-21T04:57:00+05:30","Sunrise":"2026-11-21T06:11:00+05:30",
              "Dhuhr":"2026-11-21T11:56:00+05:30","Asr":"2026-11-21T16:04:00+05:30",
              "Maghrib":"2026-11-21T17:40:00+05:30","Isha":"2026-11-21T18:55:00+05:30"},
          "date":{"gregorian":{"date":"21-11-2026"}},
          "meta":{"latitude":12.820254,"longitude":80.037819,"timezone":"Asia/Kolkata","method":{"id":1},
             "school":"HANAFI","latitudeAdjustmentMethod":"ANGLE_BASED",
             "offset":{"Fajr":0,"Sunrise":0,"Dhuhr":0,"Asr":0,"Maghrib":0,"Isha":0}}}}
    """.trimIndent()
}
