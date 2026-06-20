package ua.nure.kryvko.hikeway.feature.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.domain.auth.AuthRepository
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.LoginUseCase
import ua.nure.kryvko.hikeway.domain.auth.LogoutUseCase
import ua.nure.kryvko.hikeway.domain.auth.RestoreSessionUseCase
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest
import ua.nure.kryvko.hikeway.domain.auth.SignUpUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginValidationRejectsBlankFields() {
        assertEquals("Username is required.", validateLogin("", "password"))
        assertEquals("Password is required.", validateLogin("user", ""))
    }

    @Test
    fun signupValidationRejectsInvalidInputs() {
        assertEquals("Email is invalid.", validateSignUp(validSignUpState(email = "bad")))
        assertEquals(
            "Password must be at least 8 characters long.",
            validateSignUp(validSignUpState(password = "Aa1")),
        )
        assertEquals(
            "Username must be no longer than 255 symbols.",
            validateSignUp(validSignUpState(username = "u".repeat(256))),
        )
    }

    @Test
    fun successfulLoginAuthenticates() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateUsername("roman")
        viewModel.updatePassword("Password1")
        viewModel.logIn()
        advanceUntilIdle()

        assertEquals(AuthStatus.AUTHENTICATED, viewModel.uiState.value.status)
        assertEquals(listOf("roman"), repository.logins)
    }

    @Test
    fun successfulSignupCreatesAccountThenLogsIn() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.showSignUp()
        viewModel.updateEmail("roman@example.com")
        viewModel.updateSignUpPassword("Password1")
        viewModel.updateFirstName("Roman")
        viewModel.updateLastName("Kryvko")
        viewModel.updateSignUpUsername("roman")
        viewModel.createAccount()
        advanceUntilIdle()

        assertEquals(AuthStatus.AUTHENTICATED, viewModel.uiState.value.status)
        assertEquals(listOf("roman"), repository.signups.map { it.username })
        assertEquals(listOf("roman"), repository.logins)
    }

    @Test
    fun failedLoginShowsErrorAndStaysUnauthenticated() = runTest(dispatcher) {
        repository.failLogin = true
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.updateUsername("roman")
        viewModel.updatePassword("wrong")
        viewModel.logIn()
        advanceUntilIdle()

        assertEquals(AuthStatus.UNAUTHENTICATED, viewModel.uiState.value.status)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun logoutClearsAuthenticatedState() = runTest(dispatcher) {
        repository.restoredSession = session("roman")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.logOut()
        advanceUntilIdle()

        assertEquals(AuthStatus.UNAUTHENTICATED, viewModel.uiState.value.status)
        assertEquals(true, repository.didLogout)
    }

    private fun viewModel() = AuthViewModel(
        login = LoginUseCase(repository),
        signUp = SignUpUseCase(repository),
        restoreSession = RestoreSessionUseCase(repository),
        logout = LogoutUseCase(repository),
    )
}

private class FakeAuthRepository : AuthRepository {
    val logins = mutableListOf<String>()
    val signups = mutableListOf<SignUpRequest>()
    var failLogin = false
    var didLogout = false
    var restoredSession: AuthSession? = null

    override suspend fun currentSession(): AuthSession? = restoredSession

    override suspend fun login(username: String, password: String): AuthSession {
        if (failLogin) error("Invalid credentials")
        logins += username
        return session(username)
    }

    override suspend fun signUp(request: SignUpRequest) {
        signups += request
    }

    override suspend fun refreshSession(): AuthSession? = null

    override suspend fun logout() {
        didLogout = true
        restoredSession = null
    }
}

private fun validSignUpState(
    email: String = "roman@example.com",
    password: String = "Password1",
    username: String = "roman",
) = AuthUiState(
    email = email,
    signUpPassword = password,
    firstName = "Roman",
    lastName = "Kryvko",
    signUpUsername = username,
)

private fun session(username: String) = AuthSession(
    accessToken = "access",
    refreshToken = "refresh",
    expiresAtEpochMillis = Long.MAX_VALUE,
    username = username,
    userId = "user-$username",
)
