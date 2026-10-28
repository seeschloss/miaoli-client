package im.miaoli.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TotozTest {
    @Test
    fun detectsIncompleteTotozQueryAtCursor() {
        val empty = incompleteTotozQuery(
            TextFieldValue("hello [:", selection = TextRange(8)),
        )
        assertEquals(TotozQuery(start = 6, query = ""), empty)

        val typed = incompleteTotozQuery(
            TextFieldValue("hello [:wou", selection = TextRange(11)),
        )
        assertEquals(TotozQuery(start = 6, query = "wou"), typed)

        val beforeClosingBracket = incompleteTotozQuery(
            TextFieldValue("hello [:]", selection = TextRange(8)),
        )
        assertEquals(TotozQuery(start = 6, query = ""), beforeClosingBracket)

        val typedBeforeClosingBracket = incompleteTotozQuery(
            TextFieldValue("hello [:wou]", selection = TextRange(11)),
        )
        assertEquals(TotozQuery(start = 6, query = "wou"), typedBeforeClosingBracket)
    }

    @Test
    fun hidesQueryWhenClosedOrSelected() {
        assertNull(
            incompleteTotozQuery(
                TextFieldValue("hello [:wou]", selection = TextRange(12)),
            ),
        )
        assertNull(
            incompleteTotozQuery(
                TextFieldValue("hello [:wou", selection = TextRange(7, 10)),
            ),
        )
        assertNull(
            incompleteTotozQuery(
                TextFieldValue("hello [:/wou", selection = TextRange(12)),
            ),
        )
    }

    @Test
    fun ranksOwnUsageThenOtherUsage() {
        val suggestions = buildTotozSuggestions(
            ownUsage = mapOf("a" to 3, "b" to 1),
            otherUsage = mapOf("b" to 100, "c" to 5, "d" to 4),
            query = "",
        )

        assertEquals(listOf("a", "b", "c", "d"), suggestions)
    }

    @Test
    fun filtersSuggestionsCaseInsensitively() {
        val suggestions = buildTotozSuggestions(
            ownUsage = mapOf("Alpha" to 3),
            otherUsage = mapOf("alphabet" to 5, "beta" to 4),
            query = "ALP",
        )

        assertEquals(listOf("Alpha", "alphabet"), suggestions)
    }

    @Test
    fun completesTotozAndPreservesFollowingText() {
        val result = completeTotoz(
            value = TextFieldValue(
                "hello [:wou world",
                selection = TextRange(11),
            ),
            start = 6,
            name = "wouhou",
        )

        assertEquals("hello [:wouhou] world", result.text)
        assertEquals(TextRange(16), result.selection)
    }

    @Test
    fun completesTotozBeforeClosingBracket() {
        val result = completeTotoz(
            value = TextFieldValue("hello [:]", selection = TextRange(8)),
            start = 6,
            name = "wouhou",
        )

        assertEquals("hello [:wouhou] ", result.text)
        assertEquals(TextRange(16), result.selection)
    }

    @Test
    fun completesTypedTotozBeforeClosingBracket() {
        val result = completeTotoz(
            value = TextFieldValue("hello [:wou]", selection = TextRange(11)),
            start = 6,
            name = "wouhou",
        )

        assertEquals("hello [:wouhou] ", result.text)
        assertEquals(TextRange(16), result.selection)
    }

    @Test
    fun triplesSmallImages() {
        assertEquals(45f, totozDisplayLongEdgeDp(15f), 0.01f)
        assertEquals(90f, totozDisplayLongEdgeDp(30f), 0.01f)
        assertEquals(147f, totozDisplayLongEdgeDp(49f), 0.01f)
    }

    @Test
    fun usesLogarithmicRangeForMediumImages() {
        assertEquals(150f, totozDisplayLongEdgeDp(50f), 0.01f)
        assertEquals(175f, totozDisplayLongEdgeDp(100f), 0.01f)
        assertEquals(200f, totozDisplayLongEdgeDp(200f), 0.01f)
    }

    @Test
    fun capsLargeImagesAtTwoHundredDp() {
        assertEquals(200f, totozDisplayLongEdgeDp(400f), 0.01f)
    }
}
