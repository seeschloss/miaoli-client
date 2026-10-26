package im.miaoli.ui

import im.miaoli.data.FeedFormat
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.ZoneId
import java.util.Locale

/** Editable string form of a [Tribune]. */
internal data class TribuneDraft(
    val id: String,
    val title: String,
    val color: String,
    val backendUrl: String,
    val format: FeedFormat,
    val timeZoneId: String,
    val postUrl: String,
    val postField: String,
    val userAgent: String,
    val cookie: String,
    val showInFeed: Boolean,
) {
    fun validate(): Map<String, String> = buildMap {
        if (title.isBlank()) {
            put("title", "Title is required")
        }
        if (parseHexColor(color) == null) {
            put("color", "Use a #RRGGBB color")
        }
        if (!backendUrl.trim().isValidHttpUrl()) {
            put("backendUrl", "Enter an http(s) URL")
        }
        if (!timeZoneId.trim().isValidTimeZone()) {
            put("timeZoneId", "Use an IANA time zone, e.g. Europe/Paris")
        }
        if (postUrl.isNotBlank() && !postUrl.trim().isValidHttpUrl()) {
            put("postUrl", "Enter an http(s) URL or leave blank")
        }
        if (postUrl.isNotBlank() && postField.isBlank()) {
            put("postField", "Form field is required when posting")
        }
        if (userAgent.isBlank()) {
            put("userAgent", "User agent is required")
        } else if (userAgent.contains('\r') || userAgent.contains('\n')) {
            put("userAgent", "User agent cannot contain line breaks")
        }
        if (cookie.contains('\r') || cookie.contains('\n')) {
            put("cookie", "Cookie cannot contain line breaks")
        }
    }

    fun toTribune(): Tribune = Tribune(
        id = id,
        title = title.trim(),
        color = requireNotNull(parseHexColor(color)) { "Invalid color" },
        backendUrl = backendUrl.trim(),
        format = format,
        timeZoneId = timeZoneId.trim(),
        postUrl = postUrl.trim().takeIf { it.isNotEmpty() },
        postField = postField.trim().ifBlank { "message" },
        userAgent = userAgent.trim(),
        cookie = cookie.trim(),
        showInFeed = showInFeed,
    )

    companion object {
        fun from(tribune: Tribune): TribuneDraft = TribuneDraft(
            id = tribune.id,
            title = tribune.title,
            color = tribune.color.toHexColor(),
            backendUrl = tribune.backendUrl,
            format = tribune.format,
            timeZoneId = tribune.timeZoneId,
            postUrl = tribune.postUrl.orEmpty(),
            postField = tribune.postField,
            userAgent = tribune.userAgent,
            cookie = tribune.cookie,
            showInFeed = tribune.showInFeed,
        )
    }
}

/** Editable form of a complete [TribuneConfig]. */
internal data class ConfigurationDraft(
    val pollingSeconds: String,
    val tribunes: List<TribuneDraft>,
) {
    fun validate(): ConfigurationValidation {
        val seconds = pollingSeconds.toIntOrNull()
        val pollingError = if (
            seconds == null ||
            seconds !in TribuneConfig.MIN_POLLING_SECONDS..TribuneConfig.MAX_POLLING_SECONDS
        ) {
            "Use ${TribuneConfig.MIN_POLLING_SECONDS}–${TribuneConfig.MAX_POLLING_SECONDS} seconds"
        } else {
            null
        }

        val duplicateTitleIds = tribunes
            .filter { it.title.isNotBlank() }
            .groupBy { it.title.trim().lowercase(Locale.ROOT) }
            .filterValues { it.size > 1 }
            .values
            .flatten()
            .mapTo(mutableSetOf()) { it.id }

        val tribuneErrors = tribunes
            .associate { tribune ->
                val errors = tribune.validate().toMutableMap()
                if (tribune.id in duplicateTitleIds) {
                    errors["title"] = "Titles must be unique"
                }
                tribune.id to errors
            }
            .filterValues { it.isNotEmpty() }

        val config = if (pollingError == null && tribuneErrors.isEmpty() && seconds != null) {
            TribuneConfig(
                tribunes = tribunes.map { it.toTribune() },
                pollingSeconds = seconds,
            )
        } else {
            null
        }

        return ConfigurationValidation(
            pollingError = pollingError,
            tribuneErrors = tribuneErrors,
            config = config,
        )
    }

    companion object {
        fun from(config: TribuneConfig): ConfigurationDraft = ConfigurationDraft(
            pollingSeconds = config.pollingSeconds.toString(),
            tribunes = config.tribunes.map(TribuneDraft::from),
        )
    }
}

internal data class ConfigurationValidation(
    val pollingError: String?,
    val tribuneErrors: Map<String, Map<String, String>>,
    val config: TribuneConfig?,
) {
    val isValid: Boolean
        get() = config != null
}

private val hexColorPattern = Regex("^#[0-9a-fA-F]{6}$")

internal fun parseHexColor(value: String): Long? {
    val trimmed = value.trim()
    if (!hexColorPattern.matches(trimmed)) return null
    return 0xFF000000L or trimmed.substring(1).toLong(16)
}

internal fun Long.toHexColor(): String =
    String.format(Locale.US, "#%06X", this and 0xFFFFFFL)

internal fun String.isValidHttpUrl(): Boolean = trim().toHttpUrlOrNull() != null

internal fun String.isValidTimeZone(): Boolean =
    isNotBlank() && runCatching { ZoneId.of(trim()) }.isSuccess
