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
 * message older than [origin] is selected.
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
            post.clock == reference.time &&
            matchesDate(post, reference.date)
    }

    val target = if (reference.date != null) {
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

private fun matchesDate(post: Post, date: String?): Boolean {
    if (date == null) return true
    val compactDate = date.replace("-", "")
    return post.time.length >= 8 && post.time.take(8) == compactDate
}
