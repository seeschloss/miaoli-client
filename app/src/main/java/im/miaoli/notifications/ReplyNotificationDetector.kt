package im.miaoli.notifications

import im.miaoli.data.Post
import im.miaoli.data.Tribune
import im.miaoli.ui.ClockTargetResult
import im.miaoli.ui.matchesOwnPost
import im.miaoli.ui.messageClockReferences
import im.miaoli.ui.resolveClockTarget

internal const val MAX_NOTIFICATION_SEEN_POST_KEYS = 1_000

internal data class ReplyNotification(
    val reply: Post,
    val ownPost: Post,
)

internal data class ReplyScanResult(
    val notifications: List<ReplyNotification>,
    val states: Map<Long, TribuneNotificationState>,
)

/**
 * Finds replies to this device's posts that have not been seen by a previous
 * background scan. The first scan for a tribune only establishes a baseline,
 * so enabling notifications never floods the user with old answers.
 */
internal fun scanForReplyNotifications(
    posts: List<Post>,
    tribunes: List<Tribune>,
    previousStates: Map<Long, TribuneNotificationState>,
): ReplyScanResult {
    val tribunesById = tribunes.associateBy { it.id }
    val ownKeys = posts
        .filter { post ->
            tribunesById[post.tribune.id]?.matchesOwnPost(post) == true
        }
        .mapTo(mutableSetOf()) { it.key }
    val postsByTribune = posts.groupBy { it.tribune.id }
    val states = previousStates.toMutableMap()
    val notifications = mutableListOf<ReplyNotification>()

    tribunes
        .filter { it.enabled }
        .forEach { tribune ->
            val currentPosts = postsByTribune[tribune.id].orEmpty()
            val currentKeys = currentPosts.mapTo(LinkedHashSet()) { it.key }
            val previous = previousStates[tribune.id]

            if (previous == null || !previous.initialized) {
                val enabledAt = previous?.enabledAt ?: 0L
                if (tribune.notifyOnAnswers && enabledAt > 0L) {
                    currentPosts
                        .asSequence()
                        .filter { it.key !in ownKeys }
                        .filter { reply ->
                            reply.timestamp?.toEpochMilli()?.let { it >= enabledAt } == true
                        }
                        .forEach { reply ->
                            findOwnTarget(reply, posts, tribunes, ownKeys)?.let { ownPost ->
                                notifications += ReplyNotification(reply, ownPost)
                            }
                        }
                }
                states[tribune.id] = TribuneNotificationState(
                    initialized = true,
                    seenPostKeys = trimSeenKeys(currentKeys),
                    enabledAt = enabledAt,
                )
                return@forEach
            }

            if (tribune.notifyOnAnswers) {
                currentPosts
                    .asSequence()
                    .filter { it.key !in previous.seenPostKeys }
                    .filter { it.key !in ownKeys }
                    .forEach { reply ->
                        findOwnTarget(reply, posts, tribunes, ownKeys)?.let { ownPost ->
                            notifications += ReplyNotification(reply, ownPost)
                        }
                    }
            }

            val seen = LinkedHashSet(previous.seenPostKeys)
            seen.addAll(currentKeys)
            states[tribune.id] = previous.copy(
                initialized = true,
                seenPostKeys = trimSeenKeys(seen),
            )
        }

    return ReplyScanResult(
        notifications = notifications.sortedBy { it.reply.timestamp?.toEpochMilli() ?: Long.MAX_VALUE },
        states = states,
    )
}

private fun findOwnTarget(
    reply: Post,
    posts: List<Post>,
    tribunes: List<Tribune>,
    ownKeys: Set<String>,
): Post? = messageClockReferences(reply.message)
    .asSequence()
    .mapNotNull { reference ->
        (resolveClockTarget(
            origin = reply,
            reference = reference,
            tribunes = tribunes,
            posts = posts,
        ) as? ClockTargetResult.Found)?.post
    }
    .firstOrNull { it.key in ownKeys }

private fun trimSeenKeys(keys: Set<String>): Set<String> {
    if (keys.size <= MAX_NOTIFICATION_SEEN_POST_KEYS) return keys
    return keys.toList()
        .takeLast(MAX_NOTIFICATION_SEEN_POST_KEYS)
        .toSet()
}
