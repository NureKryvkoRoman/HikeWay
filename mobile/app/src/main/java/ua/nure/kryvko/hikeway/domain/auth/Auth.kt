package ua.nure.kryvko.hikeway.domain.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochMillis: Long,
    val username: String,
    val userId: String,
    val roles: Set<String> = emptySet(),
)

data class SignUpRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val username: String,
)

interface AuthRepository {
    fun observeSession(): Flow<AuthSession?>
    suspend fun currentSession(): AuthSession?
    suspend fun login(username: String, password: String): AuthSession
    suspend fun signUp(request: SignUpRequest)
    suspend fun refreshSession(): AuthSession?
    suspend fun logout()
}

interface CurrentUserProvider {
    val currentUserId: StateFlow<String?>

    fun requireCurrentUserId(): String {
        return currentUserId.value ?: error("Authenticated user is required.")
    }
}

class MutableCurrentUserProvider : CurrentUserProvider {
    private val _currentUserId = MutableStateFlow<String?>(null)
    override val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun setCurrentUserId(userId: String?) {
        _currentUserId.value = userId
    }
}

class LoginUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(username: String, password: String): AuthSession {
        return repository.login(username, password)
    }
}

class SignUpUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(request: SignUpRequest) {
        repository.signUp(request)
    }
}

class RestoreSessionUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): AuthSession? {
        return repository.currentSession() ?: repository.refreshSession()
    }
}

class LogoutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke() {
        repository.logout()
    }
}

class ObserveAuthSessionUseCase(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthSession?> = repository.observeSession()
}
