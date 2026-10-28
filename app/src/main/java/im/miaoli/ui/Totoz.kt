package im.miaoli.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.memory.MemoryCache
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Size
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.math.ln
import kotlin.math.roundToInt

internal val TOTOZ_PATTERN = Regex("""\[:[^\]/]+\]""")
internal const val MAX_TOTOZ_IMAGES = 50

/**
 * Small totoz are tripled; larger images follow a logarithmic curve from
 * 150 dp at 50 px to 200 dp at 200 px.
 */
internal fun totozDisplayLongEdgeDp(sourcePx: Float): Float {
    if (sourcePx <= 0f) return 0f
    return when {
        sourcePx < 50f -> sourcePx * 3f
        sourcePx < 200f -> 150f + 50f * ln(sourcePx / 50f) / ln(4f)
        else -> 200f
    }
}

/** A totoz reference such as `[:wouhou]` in a message. */
internal data class TotozReference(
    val text: String,
    val name: String,
    val start: Int,
    val end: Int,
)

/** A totoz being pressed, with its token bounds in the feed root. */
internal data class TotozPopup(
    val name: String,
    val anchor: Rect,
)

/** An unfinished `[:query` token at the composer cursor. */
internal data class TotozQuery(
    val start: Int,
    val query: String,
)

internal fun incompleteTotozQuery(value: TextFieldValue): TotozQuery? {
    if (value.selection.start != value.selection.end) return null

    val text = value.text
    val cursor = value.selection.start.coerceIn(0, text.length)
    val start = text.lastIndexOf("[:", cursor)
    if (start < 0) return null

    val rest = text.substring(start + 2)
    if (rest.contains(']')) return null

    val query = text.substring(start + 2, cursor)
    if (query.contains('/')) return null

    return TotozQuery(start = start, query = query)
}

internal fun buildTotozSuggestions(
    ownUsage: Map<String, Int>,
    otherUsage: Map<String, Int>,
    query: String,
    limit: Int = 10,
): List<String> {
    val matches = { name: String -> name.startsWith(query, ignoreCase = true) }
    val result = LinkedHashSet<String>()

    ownUsage.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key },
        )
        .forEach { entry ->
            if (result.size < limit && matches(entry.key)) {
                result.add(entry.key)
            }
        }

    otherUsage.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenBy { it.key },
        )
        .forEach { entry ->
            if (result.size < limit && matches(entry.key)) {
                result.add(entry.key)
            }
        }

    return result.toList()
}

internal fun completeTotoz(
    value: TextFieldValue,
    start: Int,
    name: String,
): TextFieldValue {
    val text = value.text
    val cursor = value.selection.start.coerceIn(0, text.length)
    val safeStart = start.coerceIn(0, cursor)
    val inserted = "[:$name] "

    return TextFieldValue(
        text = text.substring(0, safeStart) + inserted + text.substring(cursor),
        selection = TextRange(safeStart + inserted.length),
        composition = null,
    )
}

internal fun totozReferences(text: String): List<TotozReference> =
    TOTOZ_PATTERN.findAll(text).map { match ->
        TotozReference(
            text = match.value,
            name = match.value.substring(2, match.value.length - 1),
            start = match.range.first,
            end = match.range.last + 1,
        )
    }.toList()

internal fun totozUrl(name: String): String =
    "https://totoz.eu/img/" +
        URLEncoder.encode(name, Charsets.UTF_8.name()).replace("+", "%20")

internal fun totozRequest(context: Context, name: String): ImageRequest {
    val url = totozUrl(name)
    return ImageRequest.Builder(context)
        .data(url)
        .memoryCacheKey(url)
        .size(Size.ORIGINAL)
        .build()
}

/** Keeps only the most recently requested 50 totoz images in Coil's memory cache. */
internal object TotozImageCache {
    private val lock = Any()
    private val tracked = LinkedHashMap<String, Disposable>(
        MAX_TOTOZ_IMAGES,
        0.75f,
        true,
    )

    fun prefetch(context: Context, names: List<String>) {
        if (names.isEmpty()) return

        val imageLoader = SingletonImageLoader.get(context)
        synchronized(lock) {
            names.forEach { name ->
                val url = totozUrl(name)
                if (tracked[url] != null) {
                    return@forEach
                }

                tracked[url] = imageLoader.enqueue(
                    totozRequest(context, name),
                )
                while (tracked.size > MAX_TOTOZ_IMAGES) {
                    val eldest = tracked.entries.first()
                    tracked.remove(eldest.key)
                    eldest.value.dispose()
                    imageLoader.memoryCache?.remove(MemoryCache.Key(eldest.key))
                }
            }
        }
    }
}

/** Collects distinct totoz names, keeping the newest 50. */
internal suspend fun collectTotozNames(posts: List<Post>): List<String> =
    withContext(Dispatchers.Default) {
        val names = LinkedHashSet<String>()
        posts.asReversed().forEach { post ->
            currentCoroutineContext().ensureActive()
            renderedMessage(
                message = post.message,
                linkColor = readableLinkColor(Color(post.tribune.color)),
            ).totozReferences.forEach { reference ->
                if (!names.add(reference.name)) {
                    names.remove(reference.name)
                    names.add(reference.name)
                }
            }
        }
        names.toList().takeLast(MAX_TOTOZ_IMAGES)
    }

/** Counts totoz in other people's posts, for autocomplete fallback. */
internal suspend fun collectOtherTotozCounts(
    posts: List<Post>,
    tribunes: List<Tribune>,
): Map<String, Int> = withContext(Dispatchers.Default) {
    val tribunesById = tribunes.associateBy { it.id }
    val counts = mutableMapOf<String, Int>()

    posts.forEach { post ->
        currentCoroutineContext().ensureActive()
        if (tribunesById[post.tribune.id]?.matchesOwnPost(post) == true) {
            return@forEach
        }
        renderedMessage(
            message = post.message,
            linkColor = readableLinkColor(Color(post.tribune.color)),
        ).totozReferences.forEach { reference ->
            counts[reference.name] = (counts[reference.name] ?: 0) + 1
        }
    }

    counts
}

/**
 * Draws an undecorated image over the feed. It is intentionally not a Popup
 * window so it never takes pointer input; the press gesture stays in PostRow.
 */
@Composable
internal fun TotozOverlay(popup: TotozPopup) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val request = remember(popup.name, context) {
        totozRequest(context, popup.name)
    }
    val painter = rememberAsyncImagePainter(model = request)
    val intrinsic = painter.intrinsicSize
    if (!intrinsic.width.isFinite() || !intrinsic.height.isFinite() ||
        intrinsic.width <= 0f || intrinsic.height <= 0f
    ) {
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowWidth = with(density) { maxWidth.toPx() }
        val windowHeight = with(density) { maxHeight.toPx() }
        val sourceLongEdge = maxOf(intrinsic.width, intrinsic.height)
        val displayLongEdge = totozDisplayLongEdgeDp(sourceLongEdge).dp
        val aspectScale = displayLongEdge.value / sourceLongEdge
        val targetWidth = (intrinsic.width * aspectScale).dp
        val targetHeight = (intrinsic.height * aspectScale).dp
        val fit = minOf(
            1f,
            maxWidth.value / targetWidth.value,
            maxHeight.value / targetHeight.value,
        )
        val imageWidth = targetWidth * fit
        val imageHeight = targetHeight * fit
        val imageWidthPx = with(density) { imageWidth.toPx() }
        val imageHeightPx = with(density) { imageHeight.toPx() }

        val leftSide = popup.anchor.center.x < windowWidth / 2f
        val rawX = if (leftSide) {
            popup.anchor.left
        } else {
            popup.anchor.right - imageWidthPx
        }
        val above = popup.anchor.top - imageHeightPx
        val rawY = if (above >= 0f) above else popup.anchor.bottom
        val x = rawX.coerceIn(0f, (windowWidth - imageWidthPx).coerceAtLeast(0f))
        val y = rawY.coerceIn(0f, (windowHeight - imageHeightPx).coerceAtLeast(0f))

        Box(
            modifier = Modifier.offset {
                IntOffset(x.roundToInt(), y.roundToInt())
            },
        ) {
            AsyncImage(
                model = request,
                contentDescription = null,
                modifier = Modifier.size(imageWidth, imageHeight),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
            )
        }
    }
}
