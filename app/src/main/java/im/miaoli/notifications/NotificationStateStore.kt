package im.miaoli.notifications

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

internal data class TribuneNotificationState(
    val initialized: Boolean = false,
    val seenPostKeys: Set<String> = emptySet(),
    /** When notifications were last enabled for this tribune. */
    val enabledAt: Long = 0L,
)

internal interface NotificationStateStore {
    fun load(): Map<Long, TribuneNotificationState>
    fun save(states: Map<Long, TribuneNotificationState>)
    fun markEnabled(tribuneIds: Set<Long>, enabledAt: Long)
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
                            enabledAt = item.optLong("enabledAt", 0L),
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
                    put("enabledAt", state.enabledAt)
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

    override fun markEnabled(tribuneIds: Set<Long>, enabledAt: Long) {
        if (tribuneIds.isEmpty()) return
        val updated = load().toMutableMap()
        tribuneIds.forEach { tribuneId ->
            updated[tribuneId] = TribuneNotificationState(
                initialized = false,
                enabledAt = enabledAt,
            )
        }
        save(updated)
    }

    private companion object {
        const val PREFERENCES_NAME = "notification_state"
        const val KEY_STATES = "states"
    }
}
