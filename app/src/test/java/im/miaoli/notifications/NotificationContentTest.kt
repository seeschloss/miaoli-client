package im.miaoli.notifications

import im.miaoli.data.FeedFormat
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationContentTest {
    private val tribune = Tribune(
        id = 1L,
        title = "Test",
        color = 0xFFB8E0C2,
        backendUrl = "https://example.test/a",
        format = FeedFormat.TSV,
        timeZoneId = "UTC",
    )

    @Test
    fun prefersTheLoginAsTheNotificationAuthor() {
        val post = post(author = "fallback", authorInfo = "Mozilla/5.0 (X11)", login = "Alice")

        assertEquals("Alice", post.notificationAuthor())
    }

    @Test
    fun truncatesUserAgentInfoWhenLoginIsEmpty() {
        val post = post(author = "fallback", authorInfo = "Mozilla/5.0 (X11; Linux)", login = "")

        assertEquals("Mozilla/5.0 ", post.notificationAuthor())
    }

    private fun post(
        author: String,
        authorInfo: String,
        login: String,
    ) = Post(
        id = "1",
        time = "20260101120000",
        author = author,
        authorInfo = authorInfo,
        login = login,
        message = "12:00:00 hello",
        tribune = tribune,
    )
}
