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

internal data class PostRelationData(
    val relations: Map<String, PostRelation>,
    val answers: Map<String, List<String>>,
)

/**
 * Finds this device's posts, replies to them, and every post's direct answer
 * clocks. Parsing and resolution happen off the main thread.
 */
internal suspend fun computePostRelations(
    posts: List<Post>,
    tribunes: List<Tribune>,
): Map<String, PostRelation> = computePostRelationsWithAnswers(posts, tribunes).relations

/**
 * Computes relation badges and the reverse clock references used by the
 * expandable post details.
 */
internal suspend fun computePostRelationsWithAnswers(
    posts: List<Post>,
    tribunes: List<Tribune>,
): PostRelationData = withContext(Dispatchers.Default) {
    val tribunesById = tribunes.associateBy { it.id }
    val ownKeys = posts
        .filter { post ->
            tribunesById[post.tribune.id]?.matchesOwnPost(post) == true
        }
        .mapTo(mutableSetOf()) { it.key }

    val relations = mutableMapOf<String, PostRelation>()
    val answers = mutableMapOf<String, MutableList<AnswerClock>>()

    posts.forEach { post ->
        currentCoroutineContext().ensureActive()
        val rendered = renderedMessage(
            message = post.message,
            linkColor = readableLinkColor(Color(post.tribune.color)),
        )
        var repliesToOwn = false
        rendered.clockReferences.forEach { reference ->
            val target = (resolveClockTarget(
                origin = post,
                reference = reference,
                tribunes = tribunes,
                posts = posts,
            ) as? ClockTargetResult.Found)?.post ?: return@forEach
            if (post.key !in ownKeys && target.key in ownKeys) {
                repliesToOwn = true
            }
            val suffix = if (target.tribune.id != post.tribune.id) {
                "@${post.tribune.title}"
            } else {
                ""
            }
            answers.getOrPut(target.key) { mutableListOf() }
                .add(
                    AnswerClock(
                        clock = post.clock + suffix,
                        timestamp = post.timestamp?.toEpochMilli() ?: Long.MAX_VALUE,
                    ),
                )
        }
        val relation = when {
            post.key in ownKeys -> PostRelation.Own
            repliesToOwn -> PostRelation.ReplyToOwn
            else -> PostRelation.None
        }
        if (relation != PostRelation.None) {
            relations[post.key] = relation
        }
    }

    PostRelationData(
        relations = relations,
        answers = answers.mapValues { (_, references) ->
            references
                .distinctBy { it.clock }
                .sortedBy { it.timestamp }
                .map { it.clock }
        },
    )
}

private data class AnswerClock(
    val clock: String,
    val timestamp: Long,
)
