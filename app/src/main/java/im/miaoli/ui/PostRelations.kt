package im.miaoli.ui

import androidx.compose.ui.graphics.Color
import im.miaoli.data.Post
import im.miaoli.data.Tribune
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** How a post relates to messages sent from this device. */
internal enum class PostRelation {
    None,
    Own,
    ReplyToOwn,
}

/** Matches by received X-Post-Id, then by the configured username fallback. */
internal fun Tribune.matchesOwnPost(post: Post): Boolean {
    if (post.id in ownPostIds) return true
    val configuredUsername = username.trim()
    return configuredUsername.isNotEmpty() &&
        post.author.trim() == configuredUsername
}

/**
 * Finds this device's posts and posts whose clock references point at one of
 * them. Parsing and resolution happen off the main thread.
 */
internal suspend fun computePostRelations(
    posts: List<Post>,
    tribunes: List<Tribune>,
): Map<String, PostRelation> = withContext(Dispatchers.Default) {
    val tribunesById = tribunes.associateBy { it.id }
    val ownKeys = posts
        .filter { post ->
            tribunesById[post.tribune.id]?.matchesOwnPost(post) == true
        }
        .mapTo(mutableSetOf()) { it.key }

    if (ownKeys.isEmpty()) {
        return@withContext emptyMap()
    }

    buildMap {
        posts.forEach { post ->
            currentCoroutineContext().ensureActive()
            val relation = when {
                post.key in ownKeys -> PostRelation.Own
                else -> {
                    val rendered = renderedMessage(
                        message = post.message,
                        linkColor = readableLinkColor(Color(post.tribune.color)),
                    )
                    val repliesToOwn = rendered.clockReferences.any { reference ->
                        val target = resolveClockTarget(
                            origin = post,
                            reference = reference,
                            tribunes = tribunes,
                            posts = posts,
                        )
                        target is ClockTargetResult.Found && target.post.key in ownKeys
                    }
                    if (repliesToOwn) PostRelation.ReplyToOwn else PostRelation.None
                }
            }
            if (relation != PostRelation.None) {
                put(post.key, relation)
            }
        }
    }
}
