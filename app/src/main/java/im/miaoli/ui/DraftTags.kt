package im.miaoli.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Inserts a tag pair around the selection, or an empty pair at the cursor.
 * The cursor is placed inside a new empty pair and after a wrapped selection.
 */
internal fun insertTag(value: TextFieldValue, tag: String): TextFieldValue {
    val text = value.text
    val start = minOf(value.selection.start, value.selection.end)
        .coerceIn(0, text.length)
    val end = maxOf(value.selection.start, value.selection.end)
        .coerceIn(0, text.length)
    val opening = "<$tag>"
    val closing = "</$tag>"

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
