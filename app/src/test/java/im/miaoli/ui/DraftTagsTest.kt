package im.miaoli.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftTagsTest {
    @Test
    fun insertsEmptyPairAtCursor() {
        val result = insertPair(
            TextFieldValue("hello world", selection = TextRange(5)),
            "<b>",
            "</b>",
        )

        assertEquals("hello<b></b> world", result.text)
        assertEquals(TextRange(8), result.selection)
    }

    @Test
    fun wrapsSelection() {
        val result = insertPair(
            TextFieldValue("hello world", selection = TextRange(6, 11)),
            "<i>",
            "</i>",
        )

        assertEquals("hello <i>world</i>", result.text)
        assertEquals(TextRange(18), result.selection)
    }

    @Test
    fun wrapsReversedSelection() {
        val result = insertPair(
            TextFieldValue("hello world", selection = TextRange(11, 6)),
            "<u>",
            "</u>",
        )

        assertEquals("hello <u>world</u>", result.text)
        assertEquals(TextRange(18), result.selection)
    }

    @Test
    fun supportsAllRequestedTags() {
        listOf("s", "b", "i", "u", "tt").forEach { tag ->
            val result = insertPair(TextFieldValue(), "<$tag>", "</$tag>")
            assertEquals("<$tag></$tag>", result.text)
        }
    }

    @Test
    fun insertsTotozEmptyPairWithCursorInside() {
        val result = insertPair(
            TextFieldValue("hello", selection = TextRange(5)),
            "[:",
            "]",
        )

        assertEquals("hello[:]", result.text)
        assertEquals(TextRange(7), result.selection)
    }
}
