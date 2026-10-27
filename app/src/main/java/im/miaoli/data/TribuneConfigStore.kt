package im.miaoli.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Loads and stores the user's tribune configuration. */
interface TribuneConfigStore {
    fun load(): TribuneConfig
    fun save(config: TribuneConfig)
}

/**
 * Small configuration store backed by app-private SharedPreferences.
 *
 * The configuration is only a few kilobytes, so a synchronous read at startup
 * is cheap and keeps the ViewModel free of an asynchronous loading state.
 */
class SharedPreferencesTribuneConfigStore(context: Context) : TribuneConfigStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun load(): TribuneConfig {
        val raw = preferences.getString(KEY_CONFIG, null) ?: return TribuneConfig()
        return runCatching { TribuneConfigJson.decode(raw) }
            .getOrElse { TribuneConfig() }
    }

    override fun save(config: TribuneConfig) {
        preferences.edit {
            putString(KEY_CONFIG, TribuneConfigJson.encode(config.normalized()))
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "tribune_config"
        const val KEY_CONFIG = "config_json"
    }
}

/** JSON representation used by [SharedPreferencesTribuneConfigStore]. */
internal object TribuneConfigJson {
    private const val VERSION = 1

    fun encode(config: TribuneConfig): String {
        val tribunes = JSONArray()
        config.tribunes.forEach { tribune ->
            tribunes.put(
                JSONObject().apply {
                    put("id", tribune.id)
                    put("title", tribune.title)
                    put("color", tribune.color)
                    put("backendUrl", tribune.backendUrl)
                    put("format", tribune.format.name)
                    put("timeZoneId", tribune.timeZoneId)
                    put("postUrl", tribune.postUrl ?: JSONObject.NULL)
                    put("loginUrl", tribune.loginUrl ?: JSONObject.NULL)
                    put("postField", tribune.postField)
                    put("userAgent", tribune.userAgent)
                    put("cookie", tribune.cookie)
                    put("enabled", tribune.enabled)
                    put(
                        "ownPostIds",
                        JSONArray().apply {
                            tribune.ownPostIds.forEach(::put)
                        },
                    )
                },
            )
        }

        return JSONObject().apply {
            put("version", VERSION)
            put("pollingSeconds", config.pollingSeconds)
            put("tribunes", tribunes)
        }.toString()
    }

    fun decode(raw: String): TribuneConfig {
        val root = JSONObject(raw)
        val array = root.optJSONArray("tribunes") ?: JSONArray()

        val tribunes = buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optLong("id")
                if (id <= 0L) continue

                val format = runCatching {
                    FeedFormat.valueOf(item.optString("format"))
                }.getOrDefault(FeedFormat.TSV)

                add(
                    Tribune(
                        id = id,
                        title = item.optString("title", id.toString()),
                        color = item.optLong("color", 0xFF888888L),
                        backendUrl = item.optString("backendUrl"),
                        format = format,
                        timeZoneId = item.optString("timeZoneId"),
                        postUrl = if (item.isNull("postUrl")) {
                            null
                        } else {
                            item.optString("postUrl").takeIf { it.isNotBlank() }
                        },
                        loginUrl = if (item.isNull("loginUrl")) {
                            null
                        } else {
                            item.optString("loginUrl").takeIf { it.isNotBlank() }
                        },
                        postField = item.optString("postField", "message"),
                        userAgent = item.optString("userAgent", Tribune.DEFAULT_USER_AGENT),
                        cookie = item.optString("cookie", ""),
                        enabled = item.optBoolean("enabled", true),
                        ownPostIds = buildList {
                            val ids = item.optJSONArray("ownPostIds") ?: JSONArray()
                            for (index in 0 until ids.length()) {
                                ids.optString(index)
                                    .takeIf { it.isNotBlank() }
                                    ?.let(::add)
                            }
                        },
                    ),
                )
            }
        }

        return TribuneConfig(
            tribunes = tribunes.ifEmpty { Tribunes.defaults },
            pollingSeconds = root.optInt(
                "pollingSeconds",
                TribuneConfig.DEFAULT_POLLING_SECONDS,
            ),
        ).normalized()
    }
}
