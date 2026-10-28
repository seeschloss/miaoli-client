package im.miaoli.ui

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.rememberAsyncImagePainter
import coil3.memory.MemoryCache
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Size
import im.miaoli.data.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.math.roundToInt

internal val TOTOZ_PATTERN = Regex("""\[:[^\]/]+\]""")
internal const val MAX_TOTOZ_IMAGES = 50

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
        val naturalWidth = with(density) { intrinsic.width.toDp() }
        val naturalHeight = with(density) { intrinsic.height.toDp() }
        val maxImageWidth = minOf(maxWidth, 320.dp)
        val maxImageHeight = minOf(maxHeight, 320.dp)
        val scale = minOf(
            1f,
            maxImageWidth.value / naturalWidth.value,
            maxImageHeight.value / naturalHeight.value,
        )
        val imageWidth = naturalWidth * scale
        val imageHeight = naturalHeight * scale
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
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(imageWidth, imageHeight),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
