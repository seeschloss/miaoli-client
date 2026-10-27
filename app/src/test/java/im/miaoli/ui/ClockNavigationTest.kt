package im.miaoli.ui

import im.miaoli.data.FeedFormat
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockNavigationTest {
    private val originTribune = tribune(1L, "Origin")
    private val otherTribune = tribune(2L, "Other")
    private val disabledTribune = tribune(3L, "Disabled", enabled = false)

    @Test
    fun choosesClosestMatchingMessageOlderThanOrigin() {
        val origin = post("origin", "20260101120000", originTribune)
        val olderClose = post("older-close", "20251231120000", originTribune)
        val olderFar = post("older-far", "20251230120000", originTribune)
        val newer = post("newer", "20260102120000", originTribune)

        val result = resolveClockTarget(
            origin = origin,
            reference = reference("12:00:00", "12:00:00"),
            tribunes = listOf(originTribune),
            posts = listOf(origin, olderClose, olderFar, newer),
        )

        assertEquals("older-close", (result as ClockTargetResult.Found).post.id)
    }

    @Test
    fun usesDateWhenSpecified() {
        val origin = post("origin", "20260101120010", originTribune)
        val wrongDate = post("wrong-date", "20251231120000", originTribune)
        val rightDate = post("right-date", "20260102120000", originTribune)

        val result = resolveClockTarget(
            origin = origin,
            reference = reference(
                text = "2026-01-02 12:00:00",
                date = "2026-01-02",
                time = "12:00:00",
            ),
            tribunes = listOf(originTribune),
            posts = listOf(origin, wrongDate, rightDate),
        )

        assertEquals("right-date", (result as ClockTargetResult.Found).post.id)
    }

    @Test
    fun resolvesSuffixToEnabledTribune() {
        val origin = post("origin", "20260101120010", originTribune)
        val target = post("target", "20260101115900", otherTribune)

        val result = resolveClockTarget(
            origin = origin,
            reference = reference("11:59:00", "11:59:00", title = "Other"),
            tribunes = listOf(originTribune, otherTribune),
            posts = listOf(origin, target),
        )

        assertEquals("target", (result as ClockTargetResult.Found).post.id)
    }

    @Test
    fun treatsDisabledTribuneAsMissing() {
        val origin = post("origin", "20260101120010", originTribune)

        val result = resolveClockTarget(
            origin = origin,
            reference = reference("12:00:00", "12:00:00", title = "Disabled"),
            tribunes = listOf(originTribune, disabledTribune),
            posts = emptyList(),
        )

        assertEquals("Disabled", (result as ClockTargetResult.NoTribune).title)
    }

    @Test
    fun reportsMissingMessage() {
        val origin = post("origin", "20260101120010", originTribune)

        val result = resolveClockTarget(
            origin = origin,
            reference = reference("12:00:00", "12:00:00"),
            tribunes = listOf(originTribune),
            posts = listOf(origin),
        )

        assertTrue(result is ClockTargetResult.NoMessage)
    }

    @Test
    fun closureIncludesOriginAndTransitiveReferences() {
        val origin = post(
            id = "origin",
            time = "20260103120000",
            tribune = originTribune,
            message = "see 2026-01-02 12:00:00 and 2026-01-04 12:00:00",
        )
        val target = post(
            id = "target",
            time = "20260102120000",
            tribune = originTribune,
            message = "see 2026-01-01 12:00:00 and 2026-01-05 12:00:00",
        )
        val transitive = post(
            id = "transitive",
            time = "20260101120000",
            tribune = originTribune,
            message = "back to 2026-01-03 12:00:00",
        )
        val unrelated = post(
            id = "unrelated",
            time = "20260104120000",
            tribune = originTribune,
        )

        val keys = reachableClockPostKeys(
            origin = origin,
            reference = reference(
                text = "2026-01-02 12:00:00",
                time = "12:00:00",
                date = "2026-01-02",
            ),
            tribunes = listOf(originTribune),
            posts = listOf(origin, target, transitive, unrelated),
        )

        assertEquals(setOf(origin.key, target.key, transitive.key), keys)
    }

    @Test
    fun closureWithUnresolvedInitialReferenceContainsOrigin() {
        val origin = post(
            id = "origin",
            time = "20260101120010",
            tribune = originTribune,
        )

        val keys = reachableClockPostKeys(
            origin = origin,
            reference = reference(
                text = "2026-01-02 12:00:00",
                time = "12:00:00",
                date = "2026-01-02",
            ),
            tribunes = listOf(originTribune),
            posts = listOf(origin),
        )

        assertEquals(setOf(origin.key), keys)
    }

    private fun tribune(
        id: Long,
        title: String,
        enabled: Boolean = true,
    ) = Tribune(
        id = id,
        title = title,
        color = 0,
        backendUrl = "https://example.test/$id",
        format = FeedFormat.TSV,
        enabled = enabled,
    )

    private fun post(
        id: String,
        time: String,
        tribune: Tribune,
        message: String = "message",
    ) = Post(
        id = id,
        time = time,
        author = "author",
        message = message,
        tribune = tribune,
    )

    private fun reference(
        text: String,
        time: String,
        date: String? = null,
        title: String? = null,
    ) = ClockReference(
        text = text,
        date = date,
        time = time,
        tribuneTitle = title,
        start = 0,
        end = text.length,
    )
}
