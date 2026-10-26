package im.miaoli.ui

import androidx.compose.ui.text.input.TextFieldValue

/**
 * A clock inserted into the draft, including the tribune it came from.
 *
 * [start] and [end] delimit the currently rendered token in [DraftState.text],
 * including any `@tribunename` suffix but excluding the trailing space. The
 * span is updated when surrounding text changes, and the clock is discarded
 * when an edit overlaps it.
 */
data class DraftClock(
    val originTribuneId: String,
    val clock: String,
    val text: String,
    val start: Int,
    val end: Int,
)

/**
 * The composer draft plus the clocks whose origin tribunes need to be tracked.
 *
 * [hasCursor] is false until the text field reports a value. Until then,
 * tapping a message inserts at the end rather than at an implicit offset zero.
 */
data class DraftState(
    val value: TextFieldValue = TextFieldValue(),
    val clocks: List<DraftClock> = emptyList(),
    val hasCursor: Boolean = false,
) {
    val text: String
        get() = value.text
}
