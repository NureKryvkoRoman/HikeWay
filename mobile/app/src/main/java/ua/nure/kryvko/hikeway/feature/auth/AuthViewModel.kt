package ua.nure.kryvko.hikeway.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.domain.auth.LoginUseCase
import ua.nure.kryvko.hikeway.domain.auth.LogoutUseCase
import ua.nure.kryvko.hikeway.domain.auth.ObserveAuthSessionUseCase
import ua.nure.kryvko.hikeway.domain.auth.RestoreSessionUseCase
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest
import ua.nure.kryvko.hikeway.domain.auth.SignUpUseCase

enum class AuthStatus {
    CHECKING,
    AUTHENTICATED,
    UNAUTHENTICATED,
}

enum class AuthPage {
    LOGIN,
    SIGN_UP,
}

data class AuthUiState(
    val status: AuthStatus = AuthStatus.CHECKING,
    val screen: AuthPage = AuthPage.LOGIN,
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val signUpUsername: String = "",
    val signUpPassword: String = "",
    val isAdmin: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel(
    private val login: LoginUseCase,
    private val signUp: SignUpUseCase,
    private val restoreSession: RestoreSessionUseCase,
    private val logout: LogoutUseCase,
    private val observeAuthSession: ObserveAuthSessionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
        observeSession()
    }

    fun showSignUp() {
        _uiState.update { it.copy(screen = AuthPage.SIGN_UP, errorMessage = null) }
    }

    fun showLogin() {
        _uiState.update { it.copy(screen = AuthPage.LOGIN, errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updateFirstName(value: String) {
        _uiState.update { it.copy(firstName = value, errorMessage = null) }
    }

    fun updateLastName(value: String) {
        _uiState.update { it.copy(lastName = value, errorMessage = null) }
    }

    fun updateSignUpUsername(value: String) {
        _uiState.update { it.copy(signUpUsername = value, errorMessage = null) }
    }

    fun updateSignUpPassword(value: String) {
        _uiState.update { it.copy(signUpPassword = value, errorMessage = null) }
    }

    fun logIn() {
        val state = _uiState.value
        val validationMessage = validateLogin(state.username, state.password)
        if (validationMessage != null) {
            _uiState.update { it.copy(errorMessage = validationMessage) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { login(state.username.trim(), state.password) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            status = AuthStatus.AUTHENTICATED,
                            isLoading = false,
                            errorMessage = null,
                            password = "",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            status = AuthStatus.UNAUTHENTICATED,
                            isLoading = false,
                            errorMessage = error.message ?: "Could not log in.",
                        )
                    }
                }
        }
    }

    fun createAccount() {
        val state = _uiState.value
        val validationMessage = validateSignUp(state)
        if (validationMessage != null) {
            _uiState.update { it.copy(errorMessage = validationMessage) }
            return
        }
        val request = SignUpRequest(
            email = state.email.trim(),
            password = state.signUpPassword,
            firstName = state.firstName.trim(),
            lastName = state.lastName.trim(),
            username = state.signUpUsername.trim(),
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                signUp(request)
                login(request.username, request.password)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        status = AuthStatus.AUTHENTICATED,
                        isLoading = false,
                        errorMessage = null,
                        password = "",
                        signUpPassword = "",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        status = AuthStatus.UNAUTHENTICATED,
                        isLoading = false,
                        errorMessage = error.message ?: "Could not sign up.",
                    )
                }
            }
        }
    }

    fun logOut() {
        viewModelScope.launch {
            logout()
            _uiState.value = AuthUiState(status = AuthStatus.UNAUTHENTICATED)
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            val session = restoreSession()
            _uiState.update {
                it.copy(
                    status = if (session != null) AuthStatus.AUTHENTICATED else AuthStatus.UNAUTHENTICATED,
                    username = session?.username.orEmpty(),
                    isAdmin = session?.isAdmin == true,
                )
            }
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            observeAuthSession().collect { session ->
                _uiState.update { state ->
                    if (state.status == AuthStatus.CHECKING) {
                        state
                    } else {
                        state.copy(
                            status = if (session != null) {
                                AuthStatus.AUTHENTICATED
                            } else {
                                AuthStatus.UNAUTHENTICATED
                            },
                            username = session?.username ?: state.username,
                            isAdmin = session?.isAdmin == true,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(
            login: LoginUseCase,
            signUp: SignUpUseCase,
            restoreSession: RestoreSessionUseCase,
            logout: LogoutUseCase,
            observeAuthSession: ObserveAuthSessionUseCase,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(
                    login = login,
                    signUp = signUp,
                    restoreSession = restoreSession,
                    logout = logout,
                    observeAuthSession = observeAuthSession,
                ) as T
            }
        }
    }
}

private val ua.nure.kryvko.hikeway.domain.auth.AuthSession.isAdmin: Boolean
    get() = roles.any { it == "ROLE_ADMIN" || it == "ADMIN" || it == "admin" }

fun validateLogin(username: String, password: String): String? {
    return when {
        username.isBlank() -> "Username is required."
        password.isBlank() -> "Password is required."
        else -> null
    }
}

fun validateSignUp(state: AuthUiState): String? {
    val username = state.signUpUsername.trim()
    return when {
        state.email.isBlank() -> "Email is required."
        !state.email.trim().isValidEmail() -> "Email is invalid."
        state.signUpPassword.length < 8 -> "Password must be at least 8 characters long."
        !state.signUpPassword.any(Char::isLowerCase) -> "Password must contain a lowercase letter."
        !state.signUpPassword.any(Char::isUpperCase) -> "Password must contain an uppercase letter."
        !state.signUpPassword.any(Char::isDigit) -> "Password must contain a number."
        state.firstName.isBlank() -> "First name is required."
        state.lastName.isBlank() -> "Last name is required."
        username.isBlank() -> "Username is required."
        username.length > 255 -> "Username must be no longer than 255 symbols."
        else -> null
    }
}

private fun String.isValidEmail(): Boolean {
    return Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$").matches(this)
}
