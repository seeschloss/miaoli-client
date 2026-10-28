package im.miaoli.notifications

import im.miaoli.data.FeedFormat
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReplyNotificationDetectorTest {
    private val tribune = Tribune(
        id = 1L,
        title = "A",
        color = 0xFFB8E0C2,
        backendUrl = "https://example.test/a",
        format = FeedFormat.TSV,
        timeZoneId = "UTC",
        ownPostIds = listOf("1"),
        notifyOnAnswers = true,
    )

    @Test
    fun firstScanEstablishesBaselineWithoutNotifying() {
        val result = scanForReplyNotifications(
            posts = listOf(
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune),
            previousStates = emptyMap(),
        )

        assertTrue(result.notifications.isEmpty())
        assertEquals(
            setOf("1:1", "1:2"),
            result.states.getValue(1L).seenPostKeys,
        )
    }


    @Test
    fun reportsAnAnswerReceivedBeforeTheFirstScanAfterEnabling() {
        val enabledAt = Instant.parse("2026-01-01T12:00:30Z").toEpochMilli()

        val result = scanForReplyNotifications(
            posts = listOf(
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune),
            previousStates = mapOf(
                1L to TribuneNotificationState(
                    initialized = false,
                    enabledAt = enabledAt,
                ),
            ),
        )

        assertEquals(listOf("1:2"), result.notifications.map { it.reply.key })
    }

    @Test
    fun doesNotReportAnAnswerOlderThanTheEnableTime() {
        val enabledAt = Instant.parse("2026-01-01T12:01:30Z").toEpochMilli()

        val result = scanForReplyNotifications(
            posts = listOf(
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune),
            previousStates = mapOf(
                1L to TribuneNotificationState(
                    initialized = false,
                    enabledAt = enabledAt,
                ),
            ),
        )

        assertTrue(result.notifications.isEmpty())
    }

    @Test
    fun reportsEachNewReplyOnce() {
        val baseline = scanForReplyNotifications(
            posts = listOf(
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune),
            previousStates = emptyMap(),
        )

        val firstReply = scanForReplyNotifications(
            posts = listOf(
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune),
            previousStates = baseline.states,
        )

        assertEquals(listOf("1:2"), firstReply.notifications.map { it.reply.key })

        val duplicate = scanForReplyNotifications(
            posts = listOf(
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune),
            previousStates = firstReply.states,
        )

        assertTrue(duplicate.notifications.isEmpty())
    }

    @Test
    fun advancesStateWithoutNotificationWhenDisabled() {
        val disabled = tribune.copy(notifyOnAnswers = false)
        val baseline = scanForReplyNotifications(
            posts = listOf(
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(disabled),
            previousStates = emptyMap(),
        )

        val skipped = scanForReplyNotifications(
            posts = listOf(
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(disabled),
            previousStates = baseline.states,
        )

        assertTrue(skipped.notifications.isEmpty())
        assertTrue("1:2" in skipped.states.getValue(1L).seenPostKeys)
    }

    @Test
    fun ignoresOwnPostsAndUnrelatedPosts() {
        val baseline = scanForReplyNotifications(
            posts = listOf(
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune.copy(ownPostIds = listOf("1", "2"))),
            previousStates = emptyMap(),
        ).states

        val result = scanForReplyNotifications(
            posts = listOf(
                post(id = "3", time = "20260101120300", message = "hello"),
                post(id = "2", time = "20260101120100", message = "12:00:00"),
                post(id = "1", time = "20260101120000", message = "original"),
            ),
            tribunes = listOf(tribune.copy(ownPostIds = listOf("1", "2"))),
            previousStates = baseline,
        )

        assertTrue(result.notifications.isEmpty())
    }

    private fun post(
        id: String,
        time: String,
        message: String,
        author: String = "author",
    ) = Post(
        id = id,
        time = time,
        author = author,
        message = message,
        tribune = tribune,
    )
}
