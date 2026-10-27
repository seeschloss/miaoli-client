package im.miaoli.ui

import im.miaoli.data.FeedFormat
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationDraftTest {
    private val config = TribuneConfig(
        tribunes = listOf(
            Tribune(
                id = 1L,
                title = "A",
                color = 0xFFFDE096,
                backendUrl = "https://example.test/a",
                format = FeedFormat.TSV,
                timeZoneId = "Europe/Paris",
            ),
            Tribune(
                id = 2L,
                title = "B",
                color = 0xFF67C6F2,
                backendUrl = "https://example.test/b",
                format = FeedFormat.XML_HTML_ESCAPED,
                postUrl = "https://example.test/post",
                userAgent = "Custom/1.0",
                cookie = "session=abc",
                enabled = false,
            ),
        ),
        pollingSeconds = 45,
    )

    @Test
    fun roundTripsValidConfiguration() {
        val result = ConfigurationDraft.from(config).validate()

        assertTrue(result.isValid)
        assertEquals(config, result.config)
    }

    @Test
    fun convertsColorsBothWays() {
        assertEquals("#FDE096", 0xFFFDE096.toHexColor())
        assertEquals(0xFFFDE096, parseHexColor("#FDE096"))
        assertEquals(0xFF000000, parseHexColor("#000000"))
    }

    @Test
    fun rejectsInvalidColor() {
        val result = draftWith { it.copy(color = "orange") }.validate()

        assertFalse(result.isValid)
        assertNotNull(result.tribuneErrors[1L]?.get("color"))
    }

    @Test
    fun rejectsInvalidBackendUrl() {
        val result = draftWith { it.copy(backendUrl = "not a url") }.validate()

        assertFalse(result.isValid)
        assertNotNull(result.tribuneErrors[1L]?.get("backendUrl"))
    }

    @Test
    fun rejectsInvalidTimeZone() {
        val result = draftWith { it.copy(timeZoneId = "Mars/Olympus_Mons") }.validate()

        assertFalse(result.isValid)
        assertNotNull(result.tribuneErrors[1L]?.get("timeZoneId"))
    }

    @Test
    fun acceptsUtcAndIanaTimeZones() {
        assertTrue("UTC".isValidTimeZone())
        assertTrue("Europe/Paris".isValidTimeZone())
        assertFalse("not a time zone".isValidTimeZone())
    }

    @Test
    fun allowsBlankPostUrl() {
        val result = draftWith { it.copy(postUrl = "") }.validate()

        assertTrue(result.isValid)
        assertNull(result.config!!.tribunes.first { it.id == 1L }.postUrl)
    }

    @Test
    fun rejectsPostUrlWithBlankFormField() {
        val result = draftWith { it.copy(postUrl = "https://example.test/post", postField = "") }
            .validate()

        assertFalse(result.isValid)
        assertNotNull(result.tribuneErrors[1L]?.get("postField"))
    }

    @Test
    fun rejectsCookieWithLineBreak() {
        val result = draftWith { it.copy(cookie = "a=b\nc=d") }.validate()

        assertFalse(result.isValid)
        assertNotNull(result.tribuneErrors[1L]?.get("cookie"))
    }

    @Test
    fun rejectsDuplicateTitles() {
        val draft = ConfigurationDraft.from(config).let { current ->
            current.copy(
                tribunes = current.tribunes.map { tribune ->
                    if (tribune.id == 2L) tribune.copy(title = "A") else tribune
                },
            )
        }

        val result = draft.validate()

        assertFalse(result.isValid)
        assertNotNull(result.tribuneErrors[1L]?.get("title"))
        assertNotNull(result.tribuneErrors[2L]?.get("title"))
    }

    @Test
    fun enforcesPollingBounds() {
        val tooShort = ConfigurationDraft.from(config).copy(pollingSeconds = "4").validate()
        val minimum = ConfigurationDraft.from(config).copy(pollingSeconds = "5").validate()
        val maximum = ConfigurationDraft.from(config).copy(pollingSeconds = "3600").validate()
        val tooLong = ConfigurationDraft.from(config).copy(pollingSeconds = "3601").validate()

        assertFalse(tooShort.isValid)
        assertTrue(minimum.isValid)
        assertTrue(maximum.isValid)
        assertFalse(tooLong.isValid)
    }

    private fun draftWith(
        transform: (TribuneDraft) -> TribuneDraft,
    ): ConfigurationDraft = ConfigurationDraft.from(config).let { draft ->
        draft.copy(
            tribunes = draft.tribunes.map { tribune ->
                if (tribune.id == 1L) transform(tribune) else tribune
            },
        )
    }
}
