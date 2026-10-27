package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration

private const val DATE_PATTERN = """\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])"""
private const val TIME_PATTERN = """(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d"""
private const val SUFFIX_PATTERN = """(?:@[0-9A-Za-z]+)?"""

/**
 * Matches a clock reference, optionally prefixed by a date. The suffix is
 * parsed separately so a reference's matched text stops at the first space.
 * The surrounding boundaries keep times inside URLs and longer numbers from
 * being treated as clocks.
 */
private val CLOCK_BASE_PATTERN = Regex(
    """(?<![\p{L}\p{N}_/:.-])(?:(?<date>${DATE_PATTERN}) )?(?<time>${TIME_PATTERN})(?![\p{L}\p{N}_:/.-])""",
)

/** A clock reference found in a message. */
internal data class ClockReference(
    val text: String,
    val date: String?,
    val time: String,
    val tribuneTitle: String?,
    val start: Int,
    val end: Int,
)

/** Renders a message, colouring links and clock references with [linkColor]. */
internal fun renderMessage(
    message: String,
    linkColor: Color,
    onClockClick: ((ClockReference) -> Unit)? = null,
): AnnotatedString {
    val parsed = runCatching { AnnotatedString.fromHtml(message) }
        .getOrElse { AnnotatedString(message) }
    val styled = highlightClocks(highlightLinks(parsed, linkColor), linkColor)
    if (onClockClick == null) return styled

    val references = clockReferences(styled.text)
        .filterNot { reference ->
            styled.hasLinkAnnotations(reference.start, reference.end)
        }
    if (references.isEmpty()) return styled

    val clockLinkStyles = TextLinkStyles(
        style = SpanStyle(
            color = linkColor,
            textDecoration = TextDecoration.None,
        ),
    )

    return AnnotatedString.Builder(styled).apply {
        references.forEach { reference ->
            addLink(
                LinkAnnotation.Clickable(
                    tag = reference.text,
                    styles = clockLinkStyles,
                    linkInteractionListener = { onClockClick(reference) },
                ),
                reference.start,
                reference.end,
            )
        }
    }.toAnnotatedString()
}

/** Applies [color] to every link in [text]. */
internal fun highlightLinks(text: AnnotatedString, color: Color): AnnotatedString {
    val ranges = text.getLinkAnnotations(0, text.length)
        .map { TextRange(it.start, it.end) }
    if (ranges.isEmpty()) return text

    return AnnotatedString.Builder(text).apply {
        ranges.forEach { range ->
            addStyle(SpanStyle(color = color), range.start, range.end)
        }
    }.toAnnotatedString()
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
    clockReferences(text).map { reference ->
        TextRange(reference.start, reference.end)
    }

/** Parses every clock reference in [text], including date and tribune suffix. */
internal fun clockReferences(text: String): List<ClockReference> =
    CLOCK_BASE_PATTERN.findAll(text).mapNotNull { match ->
        val baseEnd = match.range.last + 1
        val suffix = if (baseEnd < text.length && text[baseEnd] == '@') {
            SUFFIX_PATTERN.find(text, baseEnd)
                ?.takeIf { it.range.first == baseEnd }
        } else {
            null
        }
        val end = suffix?.range?.last?.plus(1) ?: baseEnd
        val time = match.groups["time"]?.value ?: return@mapNotNull null

        ClockReference(
            text = text.substring(match.range.first, end),
            date = match.groups["date"]?.value,
            time = time,
            tribuneTitle = suffix?.value?.removePrefix("@"),
            start = match.range.first,
            end = end,
        )
    }.toList()
