package im.miaoli.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlFeedParserTest {
    private val parser = XmlFeedParser()
    private val tribune = Tribunes.linuxfr

    private val body = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE board SYSTEM "tp-0.1.dtd">
        <board site="https://linuxfr.org/">
          <post time="20261024202430" id="4038087">
            <info>normal !</info>
            <message>20:22:16 Il a une &lt;b&gt;superbe&lt;/b&gt; couronne</message>
            <login>Single</login>
          </post>
          <post time="20261024202421" id="4038086">
            <info>gb3</info>
            <message>&lt;a href="https://example.org/?a=1&amp;amp;b=2"&gt;[url]&lt;/a&gt;</message>
            <login></login>
          </post>
        </board>
    """.trimIndent()

    @Test
    fun parsesPostsAndAttributes() {
        val posts = parser.parse(body, tribune)

        assertEquals(2, posts.size)

        val first = posts[0]
        assertEquals("4038087", first.id)
        assertEquals("20261024202430", first.time)
        assertEquals("Single", first.author)
        assertEquals("20:24:30", first.clock)
        assertEquals("18:24:30", first.utcClock)
        assertEquals("20:22:16 Il a une <b>superbe</b> couronne", first.message)
    }

    @Test
    fun fallsBackToInfoWhenLoginIsEmpty() {
        val posts = parser.parse(body, tribune)

        assertEquals("gb3", posts[1].author)
        assertTrue(posts[1].message.contains("&amp;"))
    }

    @Test
    fun returnsEmptyListWhenThereAreNoPosts() {
        assertTrue(parser.parse("<board />", tribune).isEmpty())
    }
}
