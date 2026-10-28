package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTextTest {
    @Test
    fun findsBareClock() {
        assertEquals(
            listOf(TextRange(3, 11)),
            clockRanges("at 12:34:56 now"),
        )
    }

    @Test
    fun findsDateAndTime() {
        assertEquals(
            listOf(TextRange(0, 19)),
            clockRanges("2026-10-25 23:30:03 si tu t'organises"),
        )
    }

    @Test
    fun findsSuffix() {
        assertEquals(
            listOf(TextRange(0, 15)),
            clockRanges("12:34:56@Moules hello"),
        )
    }

    @Test
    fun findsDateAndSuffix() {
        val text = "2026-10-25 23:30:03@Moules"

        assertEquals(
            listOf(TextRange(0, text.length)),
            clockRanges(text),
        )
    }

    @Test
    fun findsEveryClockInMessage() {
        assertEquals(
            listOf(TextRange(0, 8), TextRange(9, 17)),
            clockRanges("07:16:46 07:24:05  [:wouhou4]"),
        )
    }

    @Test
    fun ignoresInvalidClocksAndLongerTokens() {
        val texts = listOf(
            "25:00:00",
            "12:60:00",
            "12:34:60",
            "112:34:56",
            "12:34:567",
            "2026-10-26T12:34:56",
            "2026-10-26-12:34:56",
            "https://example.org/12:34:56",
        )

        texts.forEach { text ->
            assertTrue("$text should not contain a clock", clockRanges(text).isEmpty())
        }
    }

    @Test
    fun findsTotozReferences() {
        val references = totozReferences("before [:wouhou] and [:foo bar] after")

        assertEquals(listOf("[:wouhou]", "[:foo bar]"), references.map { it.text })
        assertEquals(listOf("wouhou", "foo bar"), references.map { it.name })
        assertEquals(7, references[0].start)
    }

    @Test
    fun stylesTotozWithoutUnderline() {
        val highlighted = highlightTotoz(
            AnnotatedString("before [:wouhou] after"),
            Color.Red,
        )

        assertEquals(
            listOf(TextRange(7, 16)),
            highlighted.spanStyles.map { TextRange(it.start, it.end) },
        )
        assertEquals(Color.Red, highlighted.spanStyles.single().item.color)
        assertEquals(null, highlighted.spanStyles.single().item.textDecoration)
    }

    @Test
    fun rendersTotozReferencesInMessage() {
        val rendered = renderedMessage("before [:wouhou] after", Color.Red)

        assertEquals("wouhou", rendered.totozReferences.single().name)
        assertEquals(
            listOf(TextRange(7, 16)),
            rendered.text.spanStyles.map { TextRange(it.start, it.end) },
        )
    }

    @Test
    fun buildsTotozUrls() {
        assertEquals("https://totoz.eu/img/wouhou", totozUrl("wouhou"))
        assertEquals("https://totoz.eu/img/foo%20bar", totozUrl("foo bar"))
    }

    @Test
    fun stylesOnlyClockText() {
        val highlighted = highlightClocks(
            AnnotatedString("at 12:34:56 now"),
            Color.Red,
        )

        assertEquals(
            listOf(TextRange(3, 11)),
            highlighted.spanStyles.map { TextRange(it.start, it.end) },
        )
        assertEquals(Color.Red, highlighted.spanStyles.single().item.color)
    }

    @Test
    fun keepsLinksAndHighlightsClockOutsideThem() {
        val url = "https://example.org"
        val text = "$url 12:34:56"
        val builder = AnnotatedString.Builder(text)
        builder.addLink(LinkAnnotation.Url(url), 0, url.length)

        val highlighted = highlightClocks(builder.toAnnotatedString(), Color.Red)

        assertEquals(text, highlighted.text)
        assertEquals(1, highlighted.getLinkAnnotations(0, text.length).size)
        assertEquals(
            listOf(TextRange(url.length + 1, text.length)),
            highlighted.spanStyles.map { TextRange(it.start, it.end) },
        )
    }

    @Test
    fun skipsClockTextInsideLink() {
        val text = "12:34:56"
        val builder = AnnotatedString.Builder(text)
        builder.addLink(LinkAnnotation.Url("https://example.org"), 0, text.length)

        val highlighted = highlightClocks(builder.toAnnotatedString(), Color.Red)

        assertTrue(highlighted.spanStyles.isEmpty())
        assertEquals(1, highlighted.getLinkAnnotations(0, text.length).size)
    }

    @Test
    fun rendersPlainMessageWithHighlightedClock() {
        val rendered = renderMessage("at 12:34:56 now", Color.Red)

        assertEquals("at 12:34:56 now", rendered.text)
        assertEquals(
            listOf(TextRange(3, 11)),
            rendered.spanStyles.map { TextRange(it.start, it.end) },
        )
    }

    @Test
    fun parsesClockReferenceFields() {
        val reference = clockReferences("at 2026-10-25 23:30:03@Moules now")
            .single()

        assertEquals("2026-10-25 23:30:03@Moules", reference.text)
        assertEquals("2026-10-25", reference.date)
        assertEquals("23:30:03", reference.time)
        assertEquals("Moules", reference.tribuneTitle)
        assertEquals(3, reference.start)
    }

    @Test
    fun clockSuffixStopsAtFirstSpace() {
        val reference = clockReferences("at 12:34:56@Miaoli Android now")
            .single()

        assertEquals("12:34:56@Miaoli", reference.text)
        assertEquals("Miaoli", reference.tribuneTitle)
    }

    @Test
    fun cachesRenderedMessagesByMessageAndColor() {
        val first = renderedMessage("at 12:34:56 now", Color.Red)
        val second = renderedMessage("at 12:34:56 now", Color.Red)

        assertSame(first, second)
    }

    @Test
    fun stylesLinksAndClocksWithTheSameColor() {
        val text = "[url] at 12:34:56"
        val builder = AnnotatedString.Builder(text)
        builder.addLink(LinkAnnotation.Url("https://example.org"), 0, 5)

        val rendered = highlightClocks(
            highlightLinks(builder.toAnnotatedString(), Color.Red),
            Color.Red,
        )

        assertEquals(
            listOf(TextRange(0, 5), TextRange(9, 17)),
            rendered.spanStyles.map { TextRange(it.start, it.end) },
        )
        assertTrue(rendered.spanStyles.all { it.item.color == Color.Red })
    }
}
