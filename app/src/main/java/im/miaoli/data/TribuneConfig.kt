package im.miaoli.data

import java.time.ZoneId

/**
 * Persisted application configuration.
 *
 * [tribunes] contains every configured tribune, including post-only entries
 * such as the default Miaoli Android target. [feedTribunes] and
 * [postableTribunes] expose the subsets used by the feed and composer.
 */
data class TribuneConfig(
    val tribunes: List<Tribune> = Tribunes.defaults,
    val pollingSeconds: Int = DEFAULT_POLLING_SECONDS,
) {
    val feedTribunes: List<Tribune>
        get() = tribunes.filter { it.showInFeed }

    val postableTribunes: List<Tribune>
        get() = tribunes.filter { it.canPost }

    /** Returns an unused local numeric ID for a new tribune. */
    fun nextTribuneId(): Long =
        (tribunes.maxOfOrNull { it.id } ?: 0L) + 1L

    /**
     * Clamp and clean values loaded from storage or supplied by the UI. This
     * keeps a corrupt preference file from breaking the feed or posting.
     */
    fun normalized(): TribuneConfig = copy(
        pollingSeconds = pollingSeconds.coerceIn(MIN_POLLING_SECONDS, MAX_POLLING_SECONDS),
        tribunes = tribunes
            .filter { it.id > 0L && it.backendUrl.isNotBlank() }
            .map { tribune ->
                tribune.copy(
                    title = tribune.title.trim().ifBlank { tribune.id.toString() },
                    backendUrl = tribune.backendUrl.trim(),
                    timeZoneId = tribune.timeZoneId.trim().takeIf { zone ->
                        zone.isNotEmpty() && runCatching { ZoneId.of(zone) }.isSuccess
                    } ?: Tribune.DEFAULT_TIME_ZONE,
                    postUrl = tribune.postUrl?.trim()?.takeIf { it.isNotEmpty() },
                    postField = tribune.postField.trim().ifBlank { "message" },
                    userAgent = tribune.userAgent
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim()
                        .ifBlank { Tribune.DEFAULT_USER_AGENT },
                    cookie = tribune.cookie
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim(),
                )
            }
            .ifEmpty { Tribunes.defaults },
    )

    companion object {
        const val DEFAULT_POLLING_SECONDS = 30
        const val MIN_POLLING_SECONDS = 5
        const val MAX_POLLING_SECONDS = 3_600
    }
}
