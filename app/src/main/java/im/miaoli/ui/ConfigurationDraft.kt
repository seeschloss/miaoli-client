package im.miaoli.ui

import androidx.annotation.StringRes
import im.miaoli.R
import im.miaoli.data.FeedFormat
import im.miaoli.data.Tribune
import im.miaoli.data.TribuneConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.ZoneId
import java.util.Locale

/** A localized validation message, resolved by the UI. */
internal data class ValidationMessage(
    @param:StringRes val resourceId: Int,
    val formatArgs: List<Any> = emptyList(),
)

/** Editable string form of a [Tribune]. */
internal data class TribuneDraft(
    val id: Long,
    val title: String,
    val color: String,
    val backendUrl: String,
    val format: FeedFormat,
    val timeZoneId: String,
    val postUrl: String,
    val loginUrl: String,
    val postField: String,
    val userAgent: String,
    val cookie: String,
    val enabled: Boolean,
) {
    fun validate(): Map<String, ValidationMessage> = buildMap {
        if (title.isBlank()) {
            put("title", ValidationMessage(R.string.validation_title_required))
        }
        if (parseHexColor(color) == null) {
            put("color", ValidationMessage(R.string.validation_color_format))
        }
        if (!backendUrl.trim().isValidHttpUrl()) {
            put("backendUrl", ValidationMessage(R.string.validation_http_url))
        }
        if (!timeZoneId.trim().isValidTimeZone()) {
            put("timeZoneId", ValidationMessage(R.string.validation_time_zone))
        }
        if (postUrl.isNotBlank() && !postUrl.trim().isValidHttpUrl()) {
            put("postUrl", ValidationMessage(R.string.validation_http_url_optional))
        }
        if (loginUrl.isNotBlank() && !loginUrl.trim().isValidHttpUrl()) {
            put("loginUrl", ValidationMessage(R.string.validation_http_url_optional))
        }
        if (postUrl.isNotBlank() && postField.isBlank()) {
            put("postField", ValidationMessage(R.string.validation_post_field_required))
        }
        if (userAgent.isBlank()) {
            put("userAgent", ValidationMessage(R.string.validation_user_agent_required))
        } else if (userAgent.contains('\r') || userAgent.contains('\n')) {
            put("userAgent", ValidationMessage(R.string.validation_user_agent_line_breaks))
        }
        if (cookie.contains('\r') || cookie.contains('\n')) {
            put("cookie", ValidationMessage(R.string.validation_cookie_line_breaks))
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
        loginUrl = loginUrl.trim().takeIf { it.isNotEmpty() },
        postField = postField.trim().ifBlank { "message" },
        userAgent = userAgent.trim(),
        cookie = cookie.trim(),
        enabled = enabled,
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
            loginUrl = tribune.loginUrl.orEmpty(),
            postField = tribune.postField,
            userAgent = tribune.userAgent,
            cookie = tribune.cookie,
            enabled = tribune.enabled,
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
            ValidationMessage(
                resourceId = R.string.validation_polling_range,
                formatArgs = listOf(
                    TribuneConfig.MIN_POLLING_SECONDS,
                    TribuneConfig.MAX_POLLING_SECONDS,
                ),
            )
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
                    errors["title"] = ValidationMessage(R.string.validation_titles_unique)
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
    val pollingError: ValidationMessage?,
    val tribuneErrors: Map<Long, Map<String, ValidationMessage>>,
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
