package im.miaoli.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Inserts [opening] and [closing] around the selection, or as an empty pair
 * at the cursor. The cursor goes inside a new empty pair and after a wrapped
 * selection.
 */
internal fun insertPair(
    value: TextFieldValue,
    opening: String,
    closing: String,
): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end)
        .coerceIn(0, text.length)
    val end = maxOf(value.selection.start, value.selection.end)
        .coerceIn(0, text.length)

    return if (start == end) {
        val inserted = opening + closing
        TextFieldValue(
            text = text.substring(0, start) + inserted + text.substring(end),
            selection = TextRange(start + opening.length),
            composition = null,
        )
    } else {
        val selected = text.substring(start, end)
        val inserted = opening + selected + closing
        TextFieldValue(
            text = text.substring(0, start) + inserted + text.substring(end),
            selection = TextRange(start + inserted.length),
            composition = null,
        )
    }
}
