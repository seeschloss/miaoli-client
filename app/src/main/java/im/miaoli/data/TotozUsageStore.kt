package im.miaoli.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

/** Persistent counts of totoz used in successfully sent posts. */
interface TotozUsageStore {
    fun load(): Map<String, Int>
    fun save(usage: Map<String, Int>)
}

object NoopTotozUsageStore : TotozUsageStore {
    override fun load(): Map<String, Int> = emptyMap()
    override fun save(usage: Map<String, Int>) = Unit
}

class SharedPreferencesTotozUsageStore(context: Context) : TotozUsageStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun load(): Map<String, Int> {
        val raw = preferences.getString(KEY_USAGE, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { name ->
                    val count = json.optInt(name)
                    if (count > 0) {
                        put(name, count)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    override fun save(usage: Map<String, Int>) {
        val json = JSONObject()
        usage.forEach { (name, count) ->
            if (count > 0) {
                json.put(name, count)
            }
        }
        preferences.edit {
            putString(KEY_USAGE, json.toString())
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "totoz_usage"
        const val KEY_USAGE = "counts"
    }
}
