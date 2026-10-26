package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

private const val SUPPORTED_TAGS = "b|i|u|s|tt|code"

/**
 * Complete, properly nested tag pairs. Unsupported tags, uppercase tags, and
 * unclosed tags are left as plain text.
 */
private val TAG_PATTERN = Regex(
    """<(${SUPPORTED_TAGS})(?=\s|>)([^>]*)>|</(${SUPPORTED_TAGS})\s*>""",
)

private val URL_PATTERN = Regex(
    """(?:https?|ftp|gopher|file|mms|rtsp|rtmp)://[^\s<>"']+""",
)

private val TOTOZ_PATTERN = Regex("""\[:[^\]/]+\]""")

/** Puli.js uses this grey for visible markup tags. */
internal val TAG_COLOR = Color(0xFFBBBBBB)

internal data class DraftTagPair(
    val name: String,
    val openStart: Int,
    val openEnd: Int,
    val closeStart: Int,
    val closeEnd: Int,
)

private data class TagToken(
    val name: String,
    val start: Int,
    val end: Int,
    val closing: Boolean,
)

private data class StyledRange(
    val start: Int,
    val end: Int,
    val style: SpanStyle,
)

/** Applies the composer's semi-WYSIWYG styling without changing the text. */
internal class DraftVisualTransformation(
    private val clockColor: Color,
    private val linkColor: Color,
    private val tagColor: Color = TAG_COLOR,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            text = formatDraft(
                text = text.text,
                clockColor = clockColor,
                linkColor = linkColor,
                tagColor = tagColor,
            ),
            offsetMapping = OffsetMapping.Identity,
        )
}

internal fun formatDraft(
    text: String,
    clockColor: Color,
    linkColor: Color,
    tagColor: Color = TAG_COLOR,
): AnnotatedString {
    val pairs = draftTagPairs(text)
    val tagTextRanges = pairs.flatMap { pair ->
        listOf(
            TextRange(pair.openStart, pair.openEnd),
            TextRange(pair.closeStart, pair.closeEnd),
        )
    }

    val styledRanges = mutableListOf<StyledRange>()
    pairs.forEach { pair ->
        if (pair.openEnd < pair.closeStart) {
            styledRanges.add(
                StyledRange(
                    start = pair.openEnd,
                    end = pair.closeStart,
                    style = tagStyle(pair.name),
                ),
            )
        }
    }
    tagTextRanges.forEach { range ->
        styledRanges.add(
            StyledRange(
                start = range.start,
                end = range.end,
                style = SpanStyle(color = tagColor),
            ),
        )
    }
    clockRanges(text).forEach { range ->
        if (!overlapsAny(range, tagTextRanges)) {
            styledRanges.add(
                StyledRange(
                    start = range.start,
                    end = range.end,
                    style = SpanStyle(color = clockColor),
                ),
            )
        }
    }
    urlRanges(text).forEach { range ->
        if (!overlapsAny(range, tagTextRanges)) {
            styledRanges.add(
                StyledRange(
                    start = range.start,
                    end = range.end,
                    style = SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
            )
        }
    }
    totozRanges(text).forEach { range ->
        if (!overlapsAny(range, tagTextRanges)) {
            styledRanges.add(
                StyledRange(
                    start = range.start,
                    end = range.end,
                    style = SpanStyle(color = linkColor),
                ),
            )
        }
    }

    if (styledRanges.isEmpty()) return AnnotatedString(text)

    val boundaries = sortedSetOf(0, text.length)
    styledRanges.forEach { range ->
        boundaries.add(range.start)
        boundaries.add(range.end)
    }

    val points = boundaries.toList()
    val builder = AnnotatedString.Builder(text)
    for (index in 0 until points.lastIndex) {
        val start = points[index]
        val end = points[index + 1]
        if (start == end) continue

        val active = styledRanges.filter { range ->
            range.start <= start && range.end >= end
        }
        if (active.isEmpty()) continue

        val style = mergeStyles(active.map { it.style })
        if (style != SpanStyle()) {
            builder.addStyle(style, start, end)
        }
    }
    return builder.toAnnotatedString()
}

internal fun draftTagPairs(text: String): List<DraftTagPair> {
    val tokens = TAG_PATTERN.findAll(text).map { match ->
        val closing = match.value.startsWith("</")
        TagToken(
            name = if (closing) match.groupValues[3] else match.groupValues[1],
            start = match.range.first,
            end = match.range.last + 1,
            closing = closing,
        )
    }.toList()

    val openTags = mutableListOf<TagToken>()
    val pairs = mutableListOf<DraftTagPair>()
    for (token in tokens) {
        if (!token.closing) {
            openTags.add(token)
            continue
        }

        val matchIndex = openTags.indexOfLast { it.name == token.name }
        if (matchIndex < 0) continue

        val open = openTags[matchIndex]
        // Discard unclosed tags nested inside the pair we just closed.
        openTags.subList(matchIndex, openTags.size).clear()
        pairs.add(
            DraftTagPair(
                name = token.name,
                openStart = open.start,
                openEnd = open.end,
                closeStart = token.start,
                closeEnd = token.end,
            ),
        )
    }
    return pairs
}

private fun tagStyle(name: String): SpanStyle = when (name) {
    "b" -> SpanStyle(fontWeight = FontWeight.Bold)
    "i" -> SpanStyle(fontStyle = FontStyle.Italic)
    "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
    "s" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    "tt", "code" -> SpanStyle(fontFamily = FontFamily.Monospace)
    else -> SpanStyle()
}

private fun urlRanges(text: String): List<TextRange> =
    URL_PATTERN.findAll(text).mapNotNull { match ->
        var end = match.range.last + 1
        while (end > match.range.first && text[end - 1] in ",.)]}") {
            end--
        }
        if (end > match.range.first) TextRange(match.range.first, end) else null
    }.toList()

private fun totozRanges(text: String): List<TextRange> =
    TOTOZ_PATTERN.findAll(text).map { match ->
        TextRange(match.range.first, match.range.last + 1)
    }.toList()

private fun overlapsAny(range: TextRange, ranges: List<TextRange>): Boolean =
    ranges.any { other -> range.start < other.end && other.start < range.end }

private fun mergeStyles(styles: List<SpanStyle>): SpanStyle {
    var color = Color.Unspecified
    var fontWeight: FontWeight? = null
    var fontStyle: FontStyle? = null
    var fontFamily: FontFamily? = null
    var textDecoration: TextDecoration? = null

    styles.forEach { style ->
        if (style.color != Color.Unspecified) color = style.color
        if (style.fontWeight != null) fontWeight = style.fontWeight
        if (style.fontStyle != null) fontStyle = style.fontStyle
        if (style.fontFamily != null) fontFamily = style.fontFamily
        val decoration = style.textDecoration
        if (decoration != null) {
            textDecoration = textDecoration?.plus(decoration) ?: decoration
        }
    }

    return SpanStyle(
        color = color,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        textDecoration = textDecoration,
    )
}
