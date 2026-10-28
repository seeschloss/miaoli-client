package im.miaoli.ui

import im.miaoli.data.Post
import im.miaoli.data.Tribune

internal sealed interface ClockTargetResult {
    data class Found(val post: Post) : ClockTargetResult
    data class NoTribune(val title: String) : ClockTargetResult
    data class NoMessage(val reference: ClockReference) : ClockTargetResult
}

/**
 * Resolves a clock reference from [origin] against the currently visible
 * [posts]. A suffix names an enabled tribune exactly; references to disabled
 * tribunes count as missing tribunes. Without a date, the closest matching
 * message older than [origin] is selected. A minute-only clock is accepted
 * only when its minute contains exactly one post.
 */
internal fun resolveClockTarget(
    origin: Post,
    reference: ClockReference,
    tribunes: List<Tribune>,
    posts: List<Post>,
): ClockTargetResult {
    val title = reference.tribuneTitle ?: origin.tribune.title
    val targetTribune = if (reference.tribuneTitle == null) {
        origin.tribune
    } else {
        tribunes.firstOrNull { it.enabled && it.title == reference.tribuneTitle }
    } ?: return ClockTargetResult.NoTribune(title)

    val matching = posts.filter { post ->
        post.tribune.id == targetTribune.id &&
            clockMatches(post, reference) &&
            matchesDate(post, reference.date)
    }

    val target = if (reference.isMinuteOnly) {
        resolveMinuteTarget(origin, reference, matching)
    } else if (reference.date != null) {
        matching.firstOrNull()
    } else {
        val originTimestamp = origin.timestamp
            ?: return ClockTargetResult.NoMessage(reference)
        matching
            .mapNotNull { post ->
                post.timestamp?.let { timestamp -> post to timestamp }
            }
            .filter { (_, timestamp) -> timestamp < originTimestamp }
            .maxByOrNull { (_, timestamp) -> timestamp }
            ?.first
    }

    return target?.let(ClockTargetResult::Found)
        ?: ClockTargetResult.NoMessage(reference)
}

private fun clockMatches(post: Post, reference: ClockReference): Boolean =
    if (reference.isMinuteOnly) {
        post.clock.take(5) == reference.time
    } else {
        post.clock == reference.time
    }

private fun resolveMinuteTarget(
    origin: Post,
    reference: ClockReference,
    matching: List<Post>,
): Post? {
    if (reference.date != null) {
        return matching.singleOrNull()
    }

    val originTimestamp = origin.timestamp ?: return null
    val candidate = matching
        .mapNotNull { post ->
            post.timestamp?.let { timestamp -> post to timestamp }
        }
        .filter { (_, timestamp) -> timestamp < originTimestamp }
        .maxByOrNull { (_, timestamp) -> timestamp }
        ?.first
        ?: return null

    val candidateMinute = candidate.time.take(12)
    return matching
        .filter { it.time.take(12) == candidateMinute }
        .singleOrNull()
}

/**
 * Returns the origin plus every message reachable from the clicked reference.
 * Only the first reference is taken from [origin]; subsequent clocks are
 * followed from the referenced messages. Unresolved references are ignored.
 */
internal fun reachableClockPostKeys(
    origin: Post,
    reference: ClockReference,
    tribunes: List<Tribune>,
    posts: List<Post>,
): Set<String> {
    val included = linkedSetOf(origin.key)
    val queue = ArrayDeque<Post>()

    fun follow(source: Post, clock: ClockReference) {
        val target = (resolveClockTarget(source, clock, tribunes, posts)
            as? ClockTargetResult.Found)
            ?.post
            ?: return
        if (included.add(target.key)) {
            queue.add(target)
        }
    }

    follow(origin, reference)
    while (queue.isNotEmpty()) {
        val post = queue.removeFirst()
        messageClockReferences(post.message).forEach { clock ->
            follow(post, clock)
        }
    }

    return included
}

private fun matchesDate(post: Post, date: String?): Boolean {
    if (date == null) return true
    val compactDate = date.replace("-", "")
    return post.time.length >= 8 && post.time.take(8) == compactDate
}

/** Returns post keys that can be quoted with `HH:mm` because their minute is unique. */
internal fun minimalQuoteClocks(posts: List<Post>): Map<String, String> =
    posts
        .filter { it.timestamp != null }
        .groupBy { post -> post.tribune.id to post.time.take(12) }
        .filterValues { minutePosts -> minutePosts.size == 1 }
        .values
        .associate { minutePosts ->
            val post = minutePosts.single()
            post.key to post.clock.take(5)
        }
