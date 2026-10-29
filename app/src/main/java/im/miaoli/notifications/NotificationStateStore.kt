package im.miaoli.notifications

import android.content.Context
import androidx.core.content.edit
import im.miaoli.data.Post
import org.json.JSONArray
import org.json.JSONObject

internal data class TribuneNotificationState(
    val initialized: Boolean = false,
    val seenPostKeys: Set<String> = emptySet(),
    /** When notifications were last enabled for this tribune. */
    val enabledAt: Long = 0L,
    /** Highest post ID actually displayed for this tribune. */
    val latestSeenPostId: Long = 0L,
)

internal interface NotificationStateStore {
    fun load(): Map<Long, TribuneNotificationState>
    fun save(states: Map<Long, TribuneNotificationState>)
    fun markEnabled(tribuneIds: Set<Long>, enabledAt: Long)
    fun markSeen(posts: Iterable<Post>)
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
                            latestSeenPostId = item.optLong("latestSeenPostId", 0L),
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
        synchronized(lock) {
            val existing = readStatesLocked()
            val merged = existing.toMutableMap()
            states.forEach { (tribuneId, state) ->
                val previousSeen = merged[tribuneId]?.latestSeenPostId ?: 0L
                merged[tribuneId] = state.copy(
                    latestSeenPostId = maxOf(previousSeen, state.latestSeenPostId),
                )
            }
            writeStatesLocked(merged)
        }
    }

    override fun markEnabled(tribuneIds: Set<Long>, enabledAt: Long) {
        if (tribuneIds.isEmpty()) return
        synchronized(lock) {
            val updated = readStatesLocked().toMutableMap()
            tribuneIds.forEach { tribuneId ->
                updated[tribuneId] = TribuneNotificationState(
                    initialized = false,
                    enabledAt = enabledAt,
                    latestSeenPostId = updated[tribuneId]?.latestSeenPostId ?: 0L,
                )
            }
            writeStatesLocked(updated)
        }
    }

    override fun markSeen(posts: Iterable<Post>) {
        val maxima = mutableMapOf<Long, Long>()
        posts.forEach { post ->
            val id = post.id.toLongOrNull() ?: return@forEach
            if (id > (maxima[post.tribune.id] ?: 0L)) {
                maxima[post.tribune.id] = id
            }
        }
        if (maxima.isEmpty()) return

        synchronized(lock) {
            val states = readStatesLocked().toMutableMap()
            var changed = false
            maxima.forEach { (tribuneId, id) ->
                val current = states[tribuneId] ?: TribuneNotificationState()
                if (id > current.latestSeenPostId) {
                    states[tribuneId] = current.copy(latestSeenPostId = id)
                    changed = true
                }
            }
            if (changed) writeStatesLocked(states)
        }
    }

    private fun readStatesLocked(): Map<Long, TribuneNotificationState> {
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
                            latestSeenPostId = item.optLong("latestSeenPostId", 0L),
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

    private fun writeStatesLocked(states: Map<Long, TribuneNotificationState>) {
        val root = JSONObject()
        states.forEach { (tribuneId, state) ->
            root.put(
                tribuneId.toString(),
                JSONObject().apply {
                    put("initialized", state.initialized)
                    put("enabledAt", state.enabledAt)
                    put("latestSeenPostId", state.latestSeenPostId)
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
        val lock = Any()
        const val PREFERENCES_NAME = "notification_state"
        const val KEY_STATES = "states"
    }
}
