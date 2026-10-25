package im.miaoli.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TribuneConfigJsonTest {
    @Test
    fun roundTripsConfiguration() {
        val config = TribuneConfig(
            tribunes = listOf(
                Tribune(
                    id = "a",
                    title = "A",
                    color = 0xFFFDE096,
                    backendUrl = "https://example.test/a",
                    format = FeedFormat.TSV,
                    postUrl = "https://example.test/post",
                    postField = "message",
                    userAgent = "Test/1.0",
                    cookie = "session=abc",
                    showInFeed = true,
                ),
                Tribune(
                    id = "b",
                    title = "B",
                    color = 0xFF67C6F2,
                    backendUrl = "https://example.test/b",
                    format = FeedFormat.XML_HTML_ESCAPED,
                    showInFeed = false,
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
                  "id": "a",
                  "title": "  A  ",
                  "color": 4294951062,
                  "backendUrl": " https://example.test/a ",
                  "format": "TSV",
                  "postUrl": "",
                  "postField": "",
                  "userAgent": "",
                  "cookie": "  x  ",
                  "showInFeed": true
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(TribuneConfig.MIN_POLLING_SECONDS, decoded.pollingSeconds)
        val tribune = decoded.tribunes.single()
        assertEquals("A", tribune.title)
        assertEquals("https://example.test/a", tribune.backendUrl)
        assertNull(tribune.postUrl)
        assertEquals("message", tribune.postField)
        assertEquals(Tribune.DEFAULT_USER_AGENT, tribune.userAgent)
        assertEquals("x", tribune.cookie)
    }
}
