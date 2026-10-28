package im.miaoli.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TribuneConfigJsonTest {
    @Test
    fun roundTripsConfiguration() {
        val config = TribuneConfig(
            tribunes = listOf(
                Tribune(
                    id = 1L,
                    title = "A",
                    color = 0xFFFDE096,
                    backendUrl = "https://example.test/a",
                    format = FeedFormat.TSV,
                    timeZoneId = "Europe/Paris",
                    postUrl = "https://example.test/post",
                    loginUrl = "https://example.test/login",
                    postField = "message",
                    userAgent = "Test/1.0",
                    cookie = "session=abc",
                    enabled = true,
                    ownPostIds = listOf("101", "102"),
                    username = "See",
                ),
                Tribune(
                    id = 2L,
                    title = "B",
                    color = 0xFF67C6F2,
                    backendUrl = "https://example.test/b",
                    format = FeedFormat.XML_RAW,
                    enabled = false,
                ),
            ),
            pollingSeconds = 45,
        )

        val decoded = TribuneConfigJson.decode(TribuneConfigJson.encode(config))

        assertEquals(config, decoded)
    }

    @Test
    fun fallsBackToDefaultsForEmptyTribuneList() {
        val decoded = TribuneConfigJson.decode(
            """{"pollingSeconds":30,"tribunes":[]}""",
        )

        assertEquals(Tribunes.defaults, decoded.tribunes)
        assertEquals(30, decoded.pollingSeconds)
    }

    @Test
    fun normalizesStoredValues() {
        val decoded = TribuneConfigJson.decode(
            """
            {
              "pollingSeconds": 1,
              "tribunes": [
                {
                  "id": 4,
                  "title": "  A  ",
                  "color": 4294951062,
                  "backendUrl": " https://example.test/a ",
                  "format": "TSV",
                  "timeZoneId": " Europe/Paris ",
                  "postUrl": "",
                  "loginUrl": " https://example.test/login ",
                  "postField": "",
                  "userAgent": "",
                  "cookie": "  x  ",
                  "enabled": true
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(TribuneConfig.MIN_POLLING_SECONDS, decoded.pollingSeconds)
        val tribune = decoded.tribunes.single()
        assertEquals(4L, tribune.id)
        assertEquals("A", tribune.title)
        assertEquals("https://example.test/a", tribune.backendUrl)
        assertEquals("Europe/Paris", tribune.timeZoneId)
        assertNull(tribune.postUrl)
        assertEquals("https://example.test/login", tribune.loginUrl)
        assertEquals("message", tribune.postField)
        assertEquals(Tribune.DEFAULT_USER_AGENT, tribune.userAgent)
        assertEquals("x", tribune.cookie)
        assertTrue(tribune.enabled)
    }
}
