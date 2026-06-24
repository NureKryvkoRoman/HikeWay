package ua.nure.kryvko.hikeway.data.sync

import android.content.Context
import java.util.UUID

class SharedPreferencesSyncMetadataStore(context: Context) : SyncMetadataStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "sync_metadata",
        Context.MODE_PRIVATE,
    )

    override fun cursor(userId: String): String? {
        return preferences.getString("cursor_$userId", null)
    }

    override fun saveCursor(userId: String, cursor: String) {
        preferences.edit().putString("cursor_$userId", cursor).apply()
    }

    override fun deviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
