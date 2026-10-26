package im.miaoli.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import im.miaoli.data.FeedFormat
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftEditorTest {
    private val moules = Tribune(
        id = "moules",
        title = "Moules",
        color = 0xFFFDE096,
        backendUrl = "https://example.test/moules",
        format = FeedFormat.TSV,
    )
    private val linuxfr = Tribune(
        id = "linuxfr",
        title = "LinuxFR",
        color = 0xFF67C6F2,
        backendUrl = "https://example.test/linuxfr",
        format = FeedFormat.TSV,
    )
    private val config = TribuneConfig(tribunes = listOf(moules, linuxfr))

    private fun draft(text: String, selection: Int = text.length): DraftState =
        DraftState(
            value = TextFieldValue(text, TextRange(selection)),
            hasCursor = true,
        )

    private fun draftWithSelection(text: String, start: Int, end: Int): DraftState =
        DraftState(
            value = TextFieldValue(text, TextRange(start, end)),
            hasCursor = true,
        )

    private fun insert(
        current: DraftState,
        origin: Tribune,
        destination: String,
        clock: String = "12:34:56",
    ): DraftState = DraftEditor.insertClock(
        current = current,
        clock = clock,
        origin = origin,
        destinationId = destination,
        config = config,
    )

    @Test
    fun insertsAtCursor() {
        val result = insert(
            current = draft("hello world", selection = 6),
            origin = moules,
            destination = moules.id,
        )

        assertEquals("hello 12:34:56 world", result.text)
        assertEquals(TextRange(15), result.value.selection)
        assertEquals(1, result.clocks.size)
        assertEquals(6, result.clocks.single().start)
        assertEquals(14, result.clocks.single().end)
        assertEquals("12:34:56", result.clocks.single().text)
    }

    @Test
    fun insertsAtEndWhenThereIsNoCursor() {
        val result = insert(
            current = DraftState(
                value = TextFieldValue("hello"),
                hasCursor = false,
            ),
            origin = moules,
            destination = moules.id,
        )

        assertEquals("hello12:34:56 ", result.text)
        assertEquals(TextRange(14), result.value.selection)
        assertEquals(5, result.clocks.single().start)
    }

    @Test
    fun replacesSelection() {
        val result = insert(
            current = draftWithSelection("hello world", start = 6, end = 11),
            origin = moules,
            destination = moules.id,
        )

        assertEquals("hello 12:34:56 ", result.text)
        assertEquals(TextRange(15), result.value.selection)
        assertEquals(6, result.clocks.single().start)
    }

    @Test
    fun insertsBareClockWhenDestinationIsOrigin() {
        val result = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = moules.id,
        )

        assertEquals("12:34:56 ", result.text)
        assertEquals(TextRange(9), result.value.selection)
        assertEquals("12:34:56", result.clocks.single().text)
    }

    @Test
    fun insertsTribuneSuffixWhenDestinationDiffers() {
        val result = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = linuxfr.id,
        )

        assertEquals("12:34:56@Moules ", result.text)
        assertEquals(TextRange(16), result.value.selection)
        assertEquals("12:34:56@Moules", result.clocks.single().text)
    }

    @Test
    fun rewritesExistingClocksWhenDestinationChanges() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = moules.id,
        )

        val suffixed = DraftEditor.rewriteClocks(inserted, linuxfr.id, config)

        assertEquals("12:34:56@Moules ", suffixed.text)
        assertEquals(TextRange(16), suffixed.value.selection)
        assertEquals("12:34:56@Moules", suffixed.clocks.single().text)

        val bare = DraftEditor.rewriteClocks(suffixed, moules.id, config)

        assertEquals("12:34:56 ", bare.text)
        assertEquals(TextRange(9), bare.value.selection)
        assertEquals("12:34:56", bare.clocks.single().text)
    }

    @Test
    fun updatesEachClockRelativeToItsOwnOrigin() {
        val first = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = moules.id,
        )
        val second = insert(
            current = first,
            origin = linuxfr,
            destination = linuxfr.id,
            clock = "12:34:57",
        )

        assertEquals("12:34:56@Moules 12:34:57 ", second.text)
        assertEquals(2, second.clocks.size)

        val backToMoules = DraftEditor.rewriteClocks(second, moules.id, config)

        assertEquals("12:34:56 12:34:57@LinuxFR ", backToMoules.text)
        assertEquals("12:34:56", backToMoules.clocks[0].text)
        assertEquals("12:34:57@LinuxFR", backToMoules.clocks[1].text)
    }

    @Test
    fun preservesCursorLogicalPositionWhenSuffixChanges() {
        val inserted = insert(
            current = draft("hello", selection = 5),
            origin = moules,
            destination = moules.id,
        )

        val moved = DraftEditor.rewriteClocks(inserted, linuxfr.id, config)

        assertEquals("hello12:34:56@Moules ", moved.text)
        assertEquals(TextRange(21), moved.value.selection)
    }

    @Test
    fun movesCursorToEndOfClockWhenItWasInside() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = moules.id,
        )
        val inside = DraftEditor.updateValue(
            current = inserted,
            newValue = inserted.value.copy(selection = TextRange(3)),
        )

        val rewritten = DraftEditor.rewriteClocks(inside, linuxfr.id, config)

        assertEquals("12:34:56@Moules ", rewritten.text)
        assertEquals(TextRange(15), rewritten.value.selection)
    }

    @Test
    fun preservesSelectionAroundRewrittenClock() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = moules.id,
        )
        val selected = DraftEditor.updateValue(
            current = inserted,
            newValue = inserted.value.copy(selection = TextRange(0, 9)),
        )

        val rewritten = DraftEditor.rewriteClocks(selected, linuxfr.id, config)

        assertEquals("12:34:56@Moules ", rewritten.text)
        assertEquals(TextRange(0, 16), rewritten.value.selection)
    }

    @Test
    fun movesSelectionInsideClockToEndOfRewrittenClock() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = moules.id,
        )
        val selected = DraftEditor.updateValue(
            current = inserted,
            newValue = inserted.value.copy(selection = TextRange(2, 6)),
        )

        val rewritten = DraftEditor.rewriteClocks(selected, linuxfr.id, config)

        assertEquals("12:34:56@Moules ", rewritten.text)
        assertEquals(TextRange(15, 15), rewritten.value.selection)
    }

    @Test
    fun typingBeforeClockKeepsReference() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = linuxfr.id,
        )
        val atStart = DraftEditor.updateValue(
            current = inserted,
            newValue = inserted.value.copy(selection = TextRange(0)),
        )

        val edited = DraftEditor.updateValue(
            current = atStart,
            newValue = TextFieldValue("hello 12:34:56@Moules ", TextRange(6)),
        )

        assertEquals("hello 12:34:56@Moules ", edited.text)
        assertEquals(1, edited.clocks.size)
        assertEquals(6, edited.clocks.single().start)
        assertEquals(21, edited.clocks.single().end)
    }

    @Test
    fun typingAfterClockKeepsReference() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = linuxfr.id,
        )

        val edited = DraftEditor.updateValue(
            current = inserted,
            newValue = TextFieldValue("12:34:56@Moules hello", TextRange(21)),
        )

        assertEquals("12:34:56@Moules hello", edited.text)
        assertEquals(1, edited.clocks.size)
        assertEquals(0, edited.clocks.single().start)
        assertEquals(15, edited.clocks.single().end)
    }

    @Test
    fun editingInsideClockRemovesReference() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = linuxfr.id,
        )
        val selected = DraftEditor.updateValue(
            current = inserted,
            newValue = inserted.value.copy(selection = TextRange(0, 8)),
        )

        val edited = DraftEditor.updateValue(
            current = selected,
            newValue = TextFieldValue("x@Moules ", TextRange(1)),
        )

        assertEquals("x@Moules ", edited.text)
        assertTrue(edited.clocks.isEmpty())
    }

    @Test
    fun backspaceInsideClockRemovesReference() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = linuxfr.id,
        )
        val atClock = DraftEditor.updateValue(
            current = inserted,
            newValue = inserted.value.copy(selection = TextRange(4)),
        )
        val newText = inserted.text.removeRange(3, 4)

        val edited = DraftEditor.updateValue(
            current = atClock,
            newValue = TextFieldValue(newText, TextRange(3)),
        )

        assertTrue(edited.clocks.isEmpty())
    }

    @Test
    fun replacingSelectionContainingClockRemovesOldReference() {
        val inserted = insert(
            current = DraftState(value = TextFieldValue(""), hasCursor = true),
            origin = moules,
            destination = linuxfr.id,
        )
        val selected = DraftEditor.updateValue(
            current = inserted,
            newValue = inserted.value.copy(selection = TextRange(0, 15)),
        )

        val replaced = insert(
            current = selected,
            origin = linuxfr,
            destination = linuxfr.id,
            clock = "12:34:57",
        )

        assertEquals("12:34:57  ", replaced.text)
        assertEquals(1, replaced.clocks.size)
        assertEquals(0, replaced.clocks.single().start)
        assertEquals(linuxfr.id, replaced.clocks.single().originTribuneId)
    }
}
