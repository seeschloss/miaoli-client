package im.miaoli.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TsvFeedParserTest {
    private val parser = TsvFeedParser()
    private val tribune = Tribunes.moules

    @Test
    fun parsesRowsAndUsesInfoWhenLoginIsBlank() {
        val body = """
            375040	20261023155241	nedflan		<s>jean-paul huchon</s>
            375041	20261023155512	cbo		osef
        """.trimIndent().replace("\n", "\r\n")

        val posts = parser.parse(body, tribune)

        assertEquals(2, posts.size)
        assertEquals("375040", posts[0].id)
        assertEquals("20261023155241", posts[0].time)
        assertEquals("nedflan", posts[0].author)
        assertEquals("<s>jean-paul huchon</s>", posts[0].message)
        assertEquals("15:52:41", posts[0].clock)
        assertEquals("cbo", posts[1].author)
    }

    @Test
    fun prefersLoginOverInfo() {
        val body = "1\t20260101120000\tinfo-name\tlogin-name\thello"

        val posts = parser.parse(body, tribune)

        assertEquals(1, posts.size)
        assertEquals("login-name", posts.single().author)
    }

    @Test
    fun keepsTabsInsideTheMessage() {
        val body = "1\t20260101120000\tinfo\tlogin\thello\tworld"

        val posts = parser.parse(body, tribune)

        assertEquals("hello\tworld", posts.single().message)
    }

    @Test
    fun skipsMalformedLines() {
        val body = """
            not-a-number	20260101120000	info	login	message
            2	20260101120000	too-few-fields
            3	20260101120000	info	login	valid
        """.trimIndent()

        val posts = parser.parse(body, tribune)

        assertEquals(1, posts.size)
        assertEquals("valid", posts.single().message)
    }

    @Test
    fun returnsEmptyListForEmptyBody() {
        assertTrue(parser.parse("", tribune).isEmpty())
    }
}
