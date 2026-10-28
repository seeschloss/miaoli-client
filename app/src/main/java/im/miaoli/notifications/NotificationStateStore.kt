package im.miaoli.notifications

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

internal data class TribuneNotificationState(
    val initialized: Boolean = false,
    val seenPostKeys: Set<String> = emptySet(),
)

internal interface NotificationStateStore {
    fun load(): Map<Long, TribuneNotificationState>
    fun save(states: Map<Long, TribuneNotificationState>)
}

/** Small per-tribune state used to avoid notifying the same answer twice. */
internal class SharedPreferencesNotificationStateStore(context: Context) :
    NotificationStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun load(): Map<Long, TribuneNotificationState> {
        val raw = preferences.getString(KEY_STATES, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { key ->
                    val tribuneId = key.toLongOrNull() ?: return@forEach
                    val item = root.optJSONObject(key) ?: return@forEach
                    put(
                        tribuneId,
                        TribuneNotificationState(
                            initialized = item.optBoolean("initialized", false),
                            seenPostKeys = buildSet {
                                val keys = item.optJSONArray("seenPostKeys") ?: JSONArray()
                                for (index in 0 until keys.length()) {
                                    keys.optString(index)
                                        .takeIf { it.isNotBlank() }
                                        ?.let(::add)
                                }
                            },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    override fun save(states: Map<Long, TribuneNotificationState>) {
        val root = JSONObject()
        states.forEach { (tribuneId, state) ->
            root.put(
                tribuneId.toString(),
                JSONObject().apply {
                    put("initialized", state.initialized)
                    put(
                        "seenPostKeys",
                        JSONArray().apply {
                            state.seenPostKeys.forEach(::put)
                        },
                    )
                },
            )
        }
        preferences.edit {
            putString(KEY_STATES, root.toString())
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "notification_state"
        const val KEY_STATES = "states"
    }
}
