package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.fromHtml

private const val DATE_PATTERN = """\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])"""
private const val TIME_PATTERN = """(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d"""
private const val SUFFIX_PATTERN = """(?:@[0-9A-Za-z]+)?"""

/**
 * Matches a clock reference, optionally prefixed by a date and optionally
 * followed by the tribune it came from. The surrounding boundaries keep times
 * inside URLs and longer numbers from being treated as clocks.
 */
private val CLOCK_PATTERN = Regex(
    """(?<![\p{L}\p{N}_/:.-])(?:(?:${DATE_PATTERN} )?${TIME_PATTERN}${SUFFIX_PATTERN})(?![\p{L}\p{N}_:/.-])""",
)

/** Renders a message, colouring clock references the same way as links. */
internal fun renderMessage(message: String, clockColor: Color): AnnotatedString {
    val parsed = runCatching { AnnotatedString.fromHtml(message) }
        .getOrElse { AnnotatedString(message) }
    return highlightClocks(parsed, clockColor)
}

/** Applies [color] to every clock reference that is not part of a link. */
internal fun highlightClocks(text: AnnotatedString, color: Color): AnnotatedString {
    val ranges = clockRanges(text.text)
        .filterNot { range -> text.hasLinkAnnotations(range.start, range.end) }
    if (ranges.isEmpty()) return text

    return AnnotatedString.Builder(text).apply {
        ranges.forEach { range ->
            addStyle(SpanStyle(color = color), range.start, range.end)
        }
    }.toAnnotatedString()
}

/** Returns the visible-text ranges that look like clock references. */
internal fun clockRanges(text: String): List<TextRange> =
    CLOCK_PATTERN.findAll(text).map { match ->
        TextRange(match.range.first, match.range.last + 1)
    }.toList()
