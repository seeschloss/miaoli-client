package im.miaoli.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftTagsTest {
    @Test
    fun insertsEmptyPairAtCursor() {
        val result = insertTag(
            TextFieldValue("hello world", selection = TextRange(5)),
            "b",
        )

        assertEquals("hello<b></b> world", result.text)
        assertEquals(TextRange(8), result.selection)
    }

    @Test
    fun wrapsSelection() {
        val result = insertTag(
            TextFieldValue("hello world", selection = TextRange(6, 11)),
            "i",
        )

        assertEquals("hello <i>world</i>", result.text)
        assertEquals(TextRange(18), result.selection)
    }

    @Test
    fun wrapsReversedSelection() {
        val result = insertTag(
            TextFieldValue("hello world", selection = TextRange(11, 6)),
            "u",
        )

        assertEquals("hello <u>world</u>", result.text)
        assertEquals(TextRange(18), result.selection)
    }

    @Test
    fun supportsAllRequestedTags() {
        listOf("s", "b", "i", "u", "tt").forEach { tag ->
            val result = insertTag(TextFieldValue(), tag)
            assertEquals("<$tag></$tag>", result.text)
        }
    }
}
