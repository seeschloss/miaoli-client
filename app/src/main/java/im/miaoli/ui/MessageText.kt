package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.fromHtml
import im.miaoli.data.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val DATE_PATTERN = """\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])"""
private const val TIME_PATTERN = """(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d"""
private val SUFFIX_PATTERN = Regex("""(?:@[0-9A-Za-z]+)?""")

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

/** A styled message and its non-link references. */
internal data class RenderedMessage(
    val text: AnnotatedString,
    val clockReferences: List<ClockReference>,
    val totozReferences: List<TotozReference>,
)

private data class MessageRenderKey(
    val message: String,
    val linkColor: Color,
)

private const val RENDER_CACHE_SIZE = 5000
private val renderCacheLock = Any()
private val renderCache = object : LinkedHashMap<MessageRenderKey, RenderedMessage>(
    RENDER_CACHE_SIZE,
    0.75f,
    true,
) {
    override fun removeEldestEntry(
        eldest: MutableMap.MutableEntry<MessageRenderKey, RenderedMessage>,
    ): Boolean = size > RENDER_CACHE_SIZE
}

/** Returns a cached styled message and its clock references. */
internal fun renderedMessage(message: String, linkColor: Color): RenderedMessage {
    val key = MessageRenderKey(message, linkColor)
    synchronized(renderCacheLock) {
        renderCache[key]?.let { return it }
    }

    val parsed = parseMessage(message)
    val styled = highlightTotoz(
        highlightClocks(highlightLinks(parsed, linkColor), linkColor),
        linkColor,
    )
    val result = RenderedMessage(
        text = styled,
        clockReferences = clockReferences(styled.text)
            .filterNot { reference ->
                styled.hasLinkAnnotations(reference.start, reference.end)
            },
        totozReferences = totozReferences(styled.text)
            .filterNot { reference ->
                styled.hasLinkAnnotations(reference.start, reference.end)
            },
    )
    synchronized(renderCacheLock) {
        renderCache[key] = result
    }
    return result
}

/** Renders a message, colouring links and clock references with [linkColor]. */
internal fun renderMessage(message: String, linkColor: Color): AnnotatedString =
    renderedMessage(message, linkColor).text

/** Warms the render cache without doing HTML parsing on the main thread. */
internal suspend fun prefetchRenderedMessages(posts: List<Post>) {
    withContext(Dispatchers.Default) {
        posts.forEach { post ->
            currentCoroutineContext().ensureActive()
            renderedMessage(
                message = post.message,
                linkColor = readableLinkColor(Color(post.tribune.color)),
            )
        }
    }
}

/** Parses HTML markup in a message into its visible text. */
internal fun parseMessage(message: String): AnnotatedString =
    runCatching { AnnotatedString.fromHtml(message) }
        .getOrElse { AnnotatedString(message) }

/** Returns clock references that are not themselves part of an HTML link. */
internal fun messageClockReferences(message: String): List<ClockReference> {
    val parsed = parseMessage(message)
    return clockReferences(parsed.text)
        .filterNot { reference ->
            parsed.hasLinkAnnotations(reference.start, reference.end)
        }
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

/** Applies [color] to every totoz reference that is not part of a link. */
internal fun highlightTotoz(text: AnnotatedString, color: Color): AnnotatedString {
    val ranges = totozReferences(text.text)
        .filterNot { reference ->
            text.hasLinkAnnotations(reference.start, reference.end)
        }
    if (ranges.isEmpty()) return text

    return AnnotatedString.Builder(text).apply {
        ranges.forEach { reference ->
            addStyle(
                SpanStyle(color = color),
                reference.start,
                reference.end,
            )
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
