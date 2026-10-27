package im.miaoli.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig

/**
 * Pure draft editing rules. Kept outside [FeedViewModel] so cursor, selection,
 * and clock-span behavior can be tested directly.
 */
internal object DraftEditor {
    /**
     * Applies a value produced by the text field. Text changes shift clock
     * spans when they happen outside a clock and discard a clock when the edit
     * overlaps it. Selection-only changes keep every reference.
     */
    fun updateValue(current: DraftState, newValue: TextFieldValue): DraftState {
        if (current.value.text == newValue.text) {
            return current.copy(value = newValue, hasCursor = true)
        }

        val edit = inferEdit(current.value, newValue)
            ?: return current.copy(
                value = newValue,
                clocks = emptyList(),
                hasCursor = true,
            )
        val isInsertion = edit.start == edit.end
        val delta = edit.inserted.length - (edit.end - edit.start)

        val clocks = current.clocks.mapNotNull { clock ->
            val overlaps = if (isInsertion) {
                clock.start < edit.start && edit.start < clock.end
            } else {
                clock.start < edit.end && edit.start < clock.end
            }
            if (overlaps) return@mapNotNull null

            val shifted = when {
                edit.end <= clock.start -> clock.copy(
                    start = clock.start + delta,
                    end = clock.end + delta,
                )
                edit.start >= clock.end -> clock
                else -> return@mapNotNull null
            }
            if (shifted.start < 0 ||
                shifted.start > shifted.end ||
                shifted.end > newValue.text.length ||
                newValue.text.substring(shifted.start, shifted.end) != shifted.text
            ) {
                null
            } else {
                shifted
            }
        }

        return current.copy(
            value = newValue,
            clocks = clocks,
            hasCursor = true,
        )
    }

    /**
     * Inserts [clock] at the current selection (or at the end when the field
     * has never reported a cursor), then rewrites every clock for
     * [destinationId]. A trailing space is inserted after the clock.
     */
    fun insertClock(
        current: DraftState,
        clock: String,
        origin: Tribune,
        destinationId: Long?,
        config: TribuneConfig,
    ): DraftState {
        val oldValue = current.value
        val oldText = oldValue.text
        val selection = oldValue.selection
        val start: Int
        val end: Int
        if (current.hasCursor) {
            start = minOf(selection.start, selection.end).coerceIn(0, oldText.length)
            end = maxOf(selection.start, selection.end).coerceIn(0, oldText.length)
        } else {
            start = oldText.length
            end = oldText.length
        }

        val token = clockToken(clock, origin.id, destinationId, config)
        val insertedText = "$token "
        val newText = buildString(oldText.length + insertedText.length) {
            append(oldText, 0, start)
            append(insertedText)
            append(oldText, end, oldText.length)
        }
        val isInsertion = start == end
        val delta = insertedText.length - (end - start)

        val shiftedClocks = current.clocks.mapNotNull { existing ->
            val overlaps = if (isInsertion) {
                existing.start < start && start < existing.end
            } else {
                existing.start < end && start < existing.end
            }
            if (overlaps) return@mapNotNull null

            when {
                end <= existing.start -> existing.copy(
                    start = existing.start + delta,
                    end = existing.end + delta,
                )
                start >= existing.end -> existing
                else -> null
            }
        }.toMutableList()

        shiftedClocks.add(
            DraftClock(
                originTribuneId = origin.id,
                clock = clock,
                text = token,
                start = start,
                end = start + token.length,
            ),
        )
        shiftedClocks.sortBy { it.start }

        val inserted = DraftState(
            value = TextFieldValue(
                text = newText,
                selection = TextRange(start + insertedText.length),
                composition = null,
            ),
            clocks = shiftedClocks,
            hasCursor = true,
        )
        return rewriteClocks(inserted, destinationId, config)
    }

    /**
     * Re-renders every tracked clock relative to [destinationId]. Clocks whose
     * origin is the destination are bare; all others get `@title`. The cursor
     * keeps its logical position, and a cursor inside a clock moves to the end
     * of that clock's rewritten token.
     */
    fun rewriteClocks(
        current: DraftState,
        destinationId: Long?,
        config: TribuneConfig,
    ): DraftState {
        if (current.clocks.isEmpty()) return current

        val oldValue = current.value
        val oldText = oldValue.text
        val sorted = current.clocks.sortedBy { it.start }

        val valid = mutableListOf<DraftClock>()
        var lastEnd = 0
        for (clock in sorted) {
            if (clock.start < lastEnd ||
                clock.start < 0 ||
                clock.end < clock.start ||
                clock.end > oldText.length ||
                oldText.substring(clock.start, clock.end) != clock.text
            ) {
                continue
            }
            valid.add(clock)
            lastEnd = clock.end
        }

        if (valid.isEmpty()) {
            return current.copy(clocks = emptyList())
        }

        val newTokens = valid.map { clock ->
            clockToken(clock.clock, clock.originTribuneId, destinationId, config)
        }
        if (valid.indices.none { index -> valid[index].text != newTokens[index] }) {
            return current.copy(clocks = valid)
        }

        val builder = StringBuilder(oldText.length)
        val entries = mutableListOf<RewriteEntry>()
        var oldPos = 0
        valid.forEachIndexed { index, clock ->
            builder.append(oldText, oldPos, clock.start)
            val token = newTokens[index]
            val newStart = builder.length
            builder.append(token)
            val newEnd = builder.length
            entries.add(RewriteEntry(clock, token, newStart, newEnd))
            oldPos = clock.end
        }
        builder.append(oldText, oldPos, oldText.length)
        val newText = builder.toString()

        fun mapPosition(position: Int): Int {
            var delta = 0
            for (entry in entries) {
                if (position <= entry.old.start) return position + delta
                if (position < entry.old.end) return entry.newEnd
                delta += (entry.newEnd - entry.newStart) -
                    (entry.old.end - entry.old.start)
            }
            return position + delta
        }

        val selection = oldValue.selection
        val mappedSelection = TextRange(
            mapPosition(minOf(selection.start, selection.end).coerceIn(0, oldText.length)),
            mapPosition(maxOf(selection.start, selection.end).coerceIn(0, oldText.length)),
        )

        return DraftState(
            value = TextFieldValue(
                text = newText,
                selection = mappedSelection,
                composition = null,
            ),
            clocks = entries.map { entry ->
                entry.old.copy(
                    text = entry.newText,
                    start = entry.newStart,
                    end = entry.newEnd,
                )
            },
            hasCursor = current.hasCursor,
        )
    }

    private data class RewriteEntry(
        val old: DraftClock,
        val newText: String,
        val newStart: Int,
        val newEnd: Int,
    )

    private data class TextEdit(
        val start: Int,
        val end: Int,
        val inserted: String,
    )

    /**
     * Infers the range replaced by a text-field update. The previous selection
     * is the best hint for normal typing and selection replacement; backspace
     * and forward-delete are handled from a collapsed cursor, with a minimal
     * diff as the fallback for IME/autocorrect edits.
     */
    private fun inferEdit(
        oldValue: TextFieldValue,
        newValue: TextFieldValue,
    ): TextEdit? {
        val oldText = oldValue.text
        val newText = newValue.text
        if (oldText == newText) return null

        val selectionStart = minOf(oldValue.selection.start, oldValue.selection.end)
            .coerceIn(0, oldText.length)
        val selectionEnd = maxOf(oldValue.selection.start, oldValue.selection.end)
            .coerceIn(0, oldText.length)
        val insertedLength = newText.length -
            (oldText.length - (selectionEnd - selectionStart))
        if (insertedLength >= 0 && selectionStart + insertedLength <= newText.length) {
            val inserted = newText.substring(selectionStart, selectionStart + insertedLength)
            if (newText == oldText.substring(0, selectionStart) +
                inserted +
                oldText.substring(selectionEnd)
            ) {
                return TextEdit(selectionStart, selectionEnd, inserted)
            }
        }

        if (oldValue.selection.start == oldValue.selection.end &&
            newText.length < oldText.length
        ) {
            val cursor = oldValue.selection.start.coerceIn(0, oldText.length)
            val deletedLength = oldText.length - newText.length
            val backspaceStart = cursor - deletedLength
            val backspaceMatches = backspaceStart >= 0 &&
                newText == oldText.removeRange(backspaceStart, cursor)
            val forwardEnd = cursor + deletedLength
            val forwardMatches = forwardEnd <= oldText.length &&
                newText == oldText.removeRange(cursor, forwardEnd)
            when {
                backspaceMatches && forwardMatches -> {
                    return if (newValue.selection.start == cursor) {
                        TextEdit(cursor, forwardEnd, "")
                    } else {
                        TextEdit(backspaceStart, cursor, "")
                    }
                }
                backspaceMatches -> return TextEdit(backspaceStart, cursor, "")
                forwardMatches -> return TextEdit(cursor, forwardEnd, "")
            }
        }

        var prefix = 0
        val maxPrefix = minOf(oldText.length, newText.length)
        while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) {
            prefix++
        }
        var suffix = 0
        val maxSuffix = minOf(oldText.length - prefix, newText.length - prefix)
        while (suffix < maxSuffix &&
            oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
        ) {
            suffix++
        }
        return TextEdit(
            start = prefix,
            end = oldText.length - suffix,
            inserted = newText.substring(prefix, newText.length - suffix),
        )
    }

    private fun clockToken(
        clock: String,
        originTribuneId: Long,
        destinationId: Long?,
        config: TribuneConfig,
    ): String {
        if (destinationId == originTribuneId) return clock
        val title = config.tribunes
            .firstOrNull { it.id == originTribuneId }
            ?.title
            ?: originTribuneId.toString()
        return "$clock@$title"
    }
}
