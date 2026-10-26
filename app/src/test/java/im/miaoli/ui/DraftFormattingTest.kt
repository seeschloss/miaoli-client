package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftFormattingTest {
    private val clockColor = Color(0xFF0000FF)
    private val linkColor = Color(0xFF1976D2)
    private val tagColor = Color(0xFFBBBBBB)

    private fun format(text: String): AnnotatedString =
        formatDraft(
            text = text,
            clockColor = clockColor,
            linkColor = linkColor,
            tagColor = tagColor,
        )

    private fun styleAt(text: AnnotatedString, offset: Int): SpanStyle =
        text.spanStyles.single { offset in it.start until it.end }.item

    private fun rangeAt(text: AnnotatedString, offset: Int): TextRange =
        text.spanStyles.single { offset in it.start until it.end }.let {
            TextRange(it.start, it.end)
        }

    @Test
    fun stylesCompleteTagPairAndKeepsText() {
        val text = "<b>bold</b>"
        val formatted = format(text)

        assertEquals(text, formatted.text)
        assertEquals(tagColor, styleAt(formatted, 0).color)
        assertNull(styleAt(formatted, 0).fontWeight)
        assertEquals(FontWeight.Bold, styleAt(formatted, 3).fontWeight)
        assertEquals(Color.Unspecified, styleAt(formatted, 3).color)
        assertEquals(tagColor, styleAt(formatted, 8).color)
    }

    @Test
    fun preservesAttributesInGreyTagText() {
        val text = "<b class=\"x\">bold</b>"
        val formatted = format(text)

        assertEquals(text, formatted.text)
        assertEquals(tagColor, styleAt(formatted, 5).color)
        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("bold")).fontWeight)
    }

    @Test
    fun leavesIncompleteUppercaseAndUnsupportedTagsAlone() {
        val texts = listOf(
            "<b>bold",
            "bold</b>",
            "<B>bold</B>",
            "<m>music</m>",
            "<span>text</span>",
            "<b/>bold</b>",
        )

        texts.forEach { text ->
            assertTrue(text, format(text).spanStyles.isEmpty())
        }
    }

    @Test
    fun stylesCodeAndTtAsMonospace() {
        val code = format("<code>x</code>")
        val tt = format("<tt>x</tt>")

        assertEquals(FontFamily.Monospace, styleAt(code, 6).fontFamily)
        assertEquals(FontFamily.Monospace, styleAt(tt, 4).fontFamily)
    }

    @Test
    fun stylesNestedTags() {
        val text = "<b>one<i>two</i>three</b>"
        val formatted = format(text)

        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("one")).fontWeight)
        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("two")).fontWeight)
        assertEquals(FontStyle.Italic, styleAt(formatted, text.indexOf("two")).fontStyle)
        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("three")).fontWeight)
        assertNull(styleAt(formatted, text.indexOf("three")).fontStyle)

        // The inner tag text inherits the outer bold but is not itself italic.
        assertEquals(tagColor, styleAt(formatted, text.indexOf("<i>")).color)
        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("<i>")).fontWeight)
        assertNull(styleAt(formatted, text.indexOf("<i>")).fontStyle)
    }

    @Test
    fun combinesUnderlineAndStrikethrough() {
        val text = "<u><s>x</s></u>"
        val formatted = format(text)

        assertEquals(
            TextDecoration.Underline + TextDecoration.LineThrough,
            styleAt(formatted, text.indexOf("x")).textDecoration,
        )
    }

    @Test
    fun stylesSameTagNestingCorrectly() {
        val text = "<b>one<b>two</b>three</b>"
        val formatted = format(text)

        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("one")).fontWeight)
        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("two")).fontWeight)
        assertEquals(FontWeight.Bold, styleAt(formatted, text.indexOf("three")).fontWeight)

        val innerOpen = text.indexOf("<b>", startIndex = 1)
        assertEquals(tagColor, styleAt(formatted, innerOpen).color)
        assertEquals(FontWeight.Bold, styleAt(formatted, innerOpen).fontWeight)

        assertEquals(tagColor, styleAt(formatted, 0).color)
        assertNull(styleAt(formatted, 0).fontWeight)
        assertEquals(tagColor, styleAt(formatted, text.lastIndexOf("</b>")).color)
        assertNull(styleAt(formatted, text.lastIndexOf("</b>")).fontWeight)
    }

    @Test
    fun stylesClocksUrlsAndTotoz() {
        val text = "12:34:56 https://example.org [:foo]"
        val formatted = format(text)
        val urlStart = text.indexOf("https")
        val totozStart = text.indexOf("[:foo]")

        assertEquals(clockColor, styleAt(formatted, 0).color)
        assertEquals(linkColor, styleAt(formatted, urlStart).color)
        assertEquals(
            TextDecoration.Underline,
            styleAt(formatted, urlStart).textDecoration,
        )
        assertEquals(linkColor, styleAt(formatted, totozStart).color)
        assertNull(styleAt(formatted, totozStart).textDecoration)
    }

    @Test
    fun trimsTrailingUrlPunctuation() {
        val text = "see https://example.org, ok"
        val formatted = format(text)
        val urlStart = text.indexOf("https")
        val comma = text.indexOf(',')

        assertEquals(TextRange(urlStart, comma), rangeAt(formatted, urlStart))
        assertTrue(formatted.spanStyles.none { comma in it.start until it.end })
    }

    @Test
    fun combinesClockAndUrlStylesWithTags() {
        val clock = format("<b>12:34:56</b>")
        val clockStyle = styleAt(clock, clock.text.indexOf("12:34:56"))
        assertEquals(clockColor, clockStyle.color)
        assertEquals(FontWeight.Bold, clockStyle.fontWeight)

        val url = format("<b>https://example.org</b>")
        val urlStyle = styleAt(url, url.text.indexOf("https"))
        assertEquals(linkColor, urlStyle.color)
        assertEquals(FontWeight.Bold, urlStyle.fontWeight)
        assertEquals(TextDecoration.Underline, urlStyle.textDecoration)
    }

    @Test
    fun doesNotStyleClocksOrUrlsInsideTagText() {
        val clockText = "<b title=\"12:34:56\">x</b>"
        val clockStyle = styleAt(format(clockText), clockText.indexOf("12"))
        assertEquals(tagColor, clockStyle.color)

        val urlText = "<b title=\"https://example.org\">x</b>"
        val urlStyle = styleAt(format(urlText), urlText.indexOf("https"))
        assertEquals(tagColor, urlStyle.color)
        assertNull(urlStyle.textDecoration)
    }

    @Test
    fun pairsPreserveTagTextIncludingAttributes() {
        val text = "<b class=\"x\">bold</b>"
        val pair = draftTagPairs(text).single()

        assertEquals("<b class=\"x\">", text.substring(pair.openStart, pair.openEnd))
        assertEquals("</b>", text.substring(pair.closeStart, pair.closeEnd))
    }

    @Test
    fun visualTransformationKeepsTextAndOffsets() {
        val transformation = DraftVisualTransformation(
            clockColor = clockColor,
            linkColor = linkColor,
            tagColor = tagColor,
        )
        val original = AnnotatedString("<b>12:34:56</b>")
        val transformed = transformation.filter(original)

        assertEquals(original.text, transformed.text.text)
        for (offset in 0..original.text.length) {
            assertEquals(offset, transformed.offsetMapping.originalToTransformed(offset))
            assertEquals(offset, transformed.offsetMapping.transformedToOriginal(offset))
        }
    }
}
