package ua.nure.kryvko.hikeway.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import ua.nure.kryvko.hikeway.domain.auth.AuthRepository
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.LoginUseCase
import ua.nure.kryvko.hikeway.domain.auth.LogoutUseCase
import ua.nure.kryvko.hikeway.domain.auth.RestoreSessionUseCase
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest
import ua.nure.kryvko.hikeway.domain.auth.SignUpUseCase
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

class AuthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsLoginFirst() {
        setContent()

        composeRule.onNodeWithText("Log in").assertIsDisplayed()
        composeRule.onNodeWithText("Username").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
    }

    @Test
    fun signUpButtonOpensSignupScreen() {
        setContent()

        composeRule.onNodeWithText("Sign Up").performClick()

        composeRule.onNodeWithText("Create account").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("First name").assertIsDisplayed()
        composeRule.onNodeWithText("Last name").assertIsDisplayed()
    }

    @Test
    fun invalidSignupShowsValidationError() {
        setContent()

        composeRule.onNodeWithText("Sign Up").performClick()
        composeRule.onNodeWithText("Email").performTextInput("bad")
        composeRule.onNodeWithText("Create account").performClick()

        composeRule.onNodeWithText("Email is invalid.").assertIsDisplayed()
    }

    private fun setContent() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(
            login = LoginUseCase(repository),
            signUp = SignUpUseCase(repository),
            restoreSession = RestoreSessionUseCase(repository),
            logout = LogoutUseCase(repository),
        )
        composeRule.setContent {
            HikeWayTheme {
                AuthScreen(viewModel)
            }
        }
    }
}

private class FakeAuthRepository : AuthRepository {
    override suspend fun currentSession(): AuthSession? = null

    override suspend fun login(username: String, password: String): AuthSession {
        return AuthSession(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochMillis = Long.MAX_VALUE,
            username = username,
            userId = "user-$username",
        )
    }

    override suspend fun signUp(request: SignUpRequest) = Unit

    override suspend fun refreshSession(): AuthSession? = null

    override suspend fun logout() = Unit
}
