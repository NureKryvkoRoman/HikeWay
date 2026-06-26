package ua.nure.kryvko.hikeway.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.nure.kryvko.hikeway.data.services.keycloak.TokenResponseDto
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

class AuthSessionFactory(
    private val timeProvider: TimeProvider,
    private val jwtDecoder: JwtDecoder,
) {
    fun create(
        response: TokenResponseDto,
        username: String,
        fallbackRefreshToken: String? = null,
    ): AuthSession {
        return AuthSession(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken ?: fallbackRefreshToken,
            expiresAtEpochMillis = timeProvider.currentTimeMillis() +
                response.expiresInSeconds.coerceAtLeast(0L) * 1_000L,
            username = username,
            userId = jwtDecoder.subject(response.accessToken),
            roles = jwtDecoder.roles(response.accessToken),
        )
    }
}

class AuthSessionManager(
    private val store: AuthSessionStore,
    private val timeProvider: TimeProvider,
    private val currentUserProvider: MutableCurrentUserProvider,
) {
    private val _activeSession = MutableStateFlow<AuthSession?>(null)
    val activeSession: StateFlow<AuthSession?> = _activeSession.asStateFlow()

    fun storedSession(): AuthSession? = store.get()

    fun validStoredSession(): AuthSession? {
        return store.get()?.takeIf { it.expiresAtEpochMillis > timeProvider.currentTimeMillis() }
    }

    fun restoreValidSession(): AuthSession? {
        return validStoredSession().also(::activate)
    }

    fun save(session: AuthSession) {
        store.save(session)
        activate(session)
    }

    fun clear() {
        store.clear()
        activate(null)
    }

    private fun activate(session: AuthSession?) {
        _activeSession.value = session
        currentUserProvider.setCurrentUserId(session?.userId)
    }
}
