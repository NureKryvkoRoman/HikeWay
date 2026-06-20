package ua.nure.kryvko.hikeway.data.auth

import android.content.Context
import ua.nure.kryvko.hikeway.domain.auth.AuthSession

interface AuthSessionStore {
    fun get(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

class SharedPreferencesAuthSessionStore(context: Context) : AuthSessionStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "auth_session",
        Context.MODE_PRIVATE,
    )

    override fun get(): AuthSession? {
        val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val username = preferences.getString(KEY_USERNAME, null) ?: return null
        val userId = preferences.getString(KEY_USER_ID, null) ?: run {
            clear()
            return null
        }
        return AuthSession(
            accessToken = accessToken,
            refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null),
            expiresAtEpochMillis = preferences.getLong(KEY_EXPIRES_AT, 0L),
            username = username,
            userId = userId,
        )
    }

    override fun save(session: AuthSession) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochMillis)
            .putString(KEY_USERNAME, session.username)
            .putString(KEY_USER_ID, session.userId)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at_epoch_millis"
        const val KEY_USERNAME = "username"
        const val KEY_USER_ID = "user_id"
    }
}
