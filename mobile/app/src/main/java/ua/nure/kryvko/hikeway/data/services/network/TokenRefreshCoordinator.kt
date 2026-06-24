package ua.nure.kryvko.hikeway.data.services.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ua.nure.kryvko.hikeway.data.auth.AuthSessionFactory
import ua.nure.kryvko.hikeway.data.auth.AuthSessionManager
import ua.nure.kryvko.hikeway.data.services.keycloak.KeycloakService
import ua.nure.kryvko.hikeway.domain.auth.AuthSession

class TokenRefreshCoordinator(
    private val keycloakService: KeycloakService,
    private val realm: String,
    private val clientId: String,
    private val sessionManager: AuthSessionManager,
    private val sessionFactory: AuthSessionFactory,
) {
    private val refreshLock = Any()

    suspend fun forceRefresh(): AuthSession? = withContext(Dispatchers.IO) {
        refreshBlocking(reuseIfTokenChangedFrom = null, allowReuse = false)
    }

    fun refreshAfterUnauthorized(failedAccessToken: String?): AuthSession? {
        return refreshBlocking(
            reuseIfTokenChangedFrom = failedAccessToken,
            allowReuse = true,
        )
    }

    private fun refreshBlocking(
        reuseIfTokenChangedFrom: String?,
        allowReuse: Boolean,
    ): AuthSession? = synchronized(refreshLock) {
        val current = sessionManager.storedSession() ?: return@synchronized null
        if (allowReuse &&
            current.accessToken != reuseIfTokenChangedFrom &&
            sessionManager.validStoredSession() != null
        ) {
            return@synchronized current
        }
        val refreshToken = current.refreshToken ?: run {
            sessionManager.clear()
            return@synchronized null
        }

        runCatching {
            val response = runBlocking {
                keycloakService.refresh(
                    realm = realm,
                    clientId = clientId,
                    refreshToken = refreshToken,
                )
            }
            sessionFactory.create(
                response = response,
                username = current.username,
                fallbackRefreshToken = refreshToken,
            ).also(sessionManager::save)
        }.getOrElse {
            sessionManager.clear()
            null
        }
    }
}
