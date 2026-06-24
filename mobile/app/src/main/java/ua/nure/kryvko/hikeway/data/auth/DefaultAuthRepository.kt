package ua.nure.kryvko.hikeway.data.auth

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import ua.nure.kryvko.hikeway.data.services.backend.BackendAuthService
import ua.nure.kryvko.hikeway.data.services.backend.toDto
import ua.nure.kryvko.hikeway.data.services.keycloak.KeycloakService
import ua.nure.kryvko.hikeway.data.services.network.TokenRefreshCoordinator
import ua.nure.kryvko.hikeway.data.services.network.toApiException
import ua.nure.kryvko.hikeway.domain.auth.AuthRepository
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest

class DefaultAuthRepository(
    private val keycloakService: KeycloakService,
    private val backendAuthService: BackendAuthService,
    private val keycloakRealm: String,
    private val keycloakClientId: String,
    private val sessionManager: AuthSessionManager,
    private val sessionFactory: AuthSessionFactory,
    private val refreshCoordinator: TokenRefreshCoordinator,
    private val gson: Gson,
    private val onAuthenticated: suspend () -> Unit = {},
) : AuthRepository {
    override fun observeSession(): Flow<AuthSession?> = sessionManager.activeSession

    override suspend fun currentSession(): AuthSession? {
        return sessionManager.restoreValidSession()
    }

    override suspend fun login(username: String, password: String): AuthSession {
        return runCatching {
            keycloakService.login(
                realm = keycloakRealm,
                clientId = keycloakClientId,
                username = username,
                password = password,
            )
        }.map { response ->
            sessionFactory.create(response, username).also(sessionManager::save)
        }.getOrElse {
            throw it.toApiException(gson, "Could not log in")
        }.also { runCatching { onAuthenticated() } }
    }

    override suspend fun signUp(request: SignUpRequest) {
        runCatching { backendAuthService.signUp(request.toDto()) }
            .getOrElse { throw it.toApiException(gson, "Could not sign up") }
    }

    override suspend fun refreshSession(): AuthSession? {
        return refreshCoordinator.forceRefresh()
            ?.also { runCatching { onAuthenticated() } }
    }

    override suspend fun logout() {
        sessionManager.clear()
    }
}
