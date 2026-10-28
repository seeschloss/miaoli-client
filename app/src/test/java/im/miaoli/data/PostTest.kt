package im.miaoli.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PostTest {
    private fun post(
        time: String,
        tribune: Tribune = Tribunes.miaoliandroid.copy(timeZoneId = "Europe/Paris"),
    ) = Post(
        id = "1",
        time = time,
        author = "author",
        message = "message",
        tribune = tribune,
    )

    @Test
    fun parsesTimestampInTribuneTimeZone() {
        val post = post("20260701150000")

        assertEquals(Instant.parse("2026-07-01T13:00:00Z"), post.timestamp)
        assertEquals("15:00:00", post.clock)
        assertEquals("2026-07-01 15:00:00", post.fullDateTime)
    }

    @Test
    fun respectsDaylightSavingTime() {
        val winter = post("20260101150000")
        val summer = post("20260701150000")

        assertEquals(Instant.parse("2026-01-01T14:00:00Z"), winter.timestamp)
        assertEquals(Instant.parse("2026-07-01T13:00:00Z"), summer.timestamp)
    }

    @Test
    fun handlesInvalidTimestamp() {
        val post = post("not-a-timestamp")

        assertNull(post.timestamp)
        assertEquals("not-a-timestamp", post.clock)
    }

    @Test
    fun withTribuneRecomputesTimestamp() {
        val paris = Tribunes.miaoliandroid.copy(timeZoneId = "Europe/Paris")
        val utc = paris.copy(timeZoneId = "UTC")
        val post = post("20260701150000", paris)

        val updated = post.withTribune(utc)

        assertEquals(Instant.parse("2026-07-01T15:00:00Z"), updated.timestamp)
    }
}
