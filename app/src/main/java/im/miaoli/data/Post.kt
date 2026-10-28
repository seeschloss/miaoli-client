package im.miaoli.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class Post(
    val id: String,
    /** Server timestamp in yyyyMMddHHmmss form, in [tribune]'s time zone. */
    val time: String,
    val author: String,
    /** Raw author info from the feed; may be a username or a user-agent. */
    val authorInfo: String = author,
    /** Login/username from the feed; blank for anonymous posts. */
    val login: String = "",
    /** Message body, possibly containing HTML markup. */
    val message: String,
    val tribune: Tribune,
    /** Absolute instant parsed from [time] using [tribune.timeZoneId]. */
    val timestamp: Instant? = parseTimestamp(time, tribune.timeZoneId),
) {
    val key: String
        get() = "${tribune.id}:$id"

    /** Clock as written by the tribune, in that tribune's local time. */
    val clock: String
        get() = if (timestamp != null && time.length >= 14) {
            "${time.substring(8, 10)}:${time.substring(10, 12)}:${time.substring(12, 14)}"
        } else {
            time
        }

    /**
     * Returns a copy carrying [tribune] and a timestamp recomputed with its
     * time zone. The generated data-class copy would keep the old timestamp.
     */
    fun withTribune(tribune: Tribune): Post =
        if (tribune == this.tribune) {
            this
        } else {
            copy(
                tribune = tribune,
                timestamp = parseTimestamp(time, tribune.timeZoneId),
            )
        }

    companion object {
        private val TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter
            .ofPattern("uuuuMMddHHmmss")
            .withResolverStyle(ResolverStyle.STRICT)

        /** Parses a server-local timestamp using an IANA time zone. */
        fun parseTimestamp(time: String, timeZoneId: String): Instant? {
            if (time.length < 14) return null
            return runCatching {
                LocalDateTime.parse(time.take(14), TIMESTAMP_FORMAT)
                    .atZone(ZoneId.of(timeZoneId))
                    .toInstant()
            }.getOrNull()
        }
    }
}
