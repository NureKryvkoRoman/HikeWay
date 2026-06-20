package ua.nure.kryvko.hikeway.data.auth

import ua.nure.kryvko.hikeway.domain.auth.AuthRepository
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

class DefaultAuthRepository(
    private val api: AuthApi,
    private val sessionStore: AuthSessionStore,
    private val timeProvider: TimeProvider,
    private val currentUserProvider: MutableCurrentUserProvider,
) : AuthRepository {
    override suspend fun currentSession(): AuthSession? {
        val session = sessionStore.get()?.takeIf { it.expiresAtEpochMillis > timeProvider.currentTimeMillis() }
        currentUserProvider.setCurrentUserId(session?.userId)
        return session
    }

    override suspend fun login(username: String, password: String): AuthSession {
        val response = api.login(username = username, password = password)
        return response.toSession(username).also(::saveSession)
    }

    override suspend fun signUp(request: SignUpRequest) {
        api.signUp(request)
    }

    override suspend fun refreshSession(): AuthSession? {
        val current = sessionStore.get() ?: return null
        val refreshToken = current.refreshToken ?: return null
        return runCatching {
            api.refresh(refreshToken).toSession(current.username).also(::saveSession)
        }.getOrElse {
            sessionStore.clear()
            currentUserProvider.setCurrentUserId(null)
            null
        }
    }

    override suspend fun logout() {
        sessionStore.clear()
        currentUserProvider.setCurrentUserId(null)
    }

    private fun saveSession(session: AuthSession) {
        sessionStore.save(session)
        currentUserProvider.setCurrentUserId(session.userId)
    }

    private fun TokenResponse.toSession(username: String): AuthSession {
        val expiresInMillis = expiresInSeconds.coerceAtLeast(0L) * 1_000L
        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtEpochMillis = timeProvider.currentTimeMillis() + expiresInMillis,
            username = username,
            userId = extractJwtSubject(accessToken),
        )
    }
}
