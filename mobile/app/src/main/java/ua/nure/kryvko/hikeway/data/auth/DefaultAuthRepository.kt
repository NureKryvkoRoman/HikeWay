package ua.nure.kryvko.hikeway.data.auth

import ua.nure.kryvko.hikeway.domain.auth.AuthRepository
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

class DefaultAuthRepository(
    private val api: AuthApi,
    private val sessionStore: AuthSessionStore,
    private val timeProvider: TimeProvider,
) : AuthRepository {
    override suspend fun currentSession(): AuthSession? {
        return sessionStore.get()?.takeIf { it.expiresAtEpochMillis > timeProvider.currentTimeMillis() }
    }

    override suspend fun login(username: String, password: String): AuthSession {
        val response = api.login(username = username, password = password)
        return response.toSession(username).also(sessionStore::save)
    }

    override suspend fun signUp(request: SignUpRequest) {
        api.signUp(request)
    }

    override suspend fun refreshSession(): AuthSession? {
        val current = sessionStore.get() ?: return null
        val refreshToken = current.refreshToken ?: return null
        return runCatching {
            api.refresh(refreshToken).toSession(current.username).also(sessionStore::save)
        }.getOrElse {
            sessionStore.clear()
            null
        }
    }

    override suspend fun logout() {
        sessionStore.clear()
    }

    private fun TokenResponse.toSession(username: String): AuthSession {
        val expiresInMillis = expiresInSeconds.coerceAtLeast(0L) * 1_000L
        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochMillis = timeProvider.currentTimeMillis() + expiresInMillis,
            username = username,
        )
    }
}
