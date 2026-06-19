package ua.nure.kryvko.hikeway.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

class AuthDataTest {
    @Test
    fun loginFormMatchesBackendScript() {
        assertEquals(
            mapOf(
                "client_id" to "backend-test",
                "grant_type" to "password",
                "username" to "admin",
                "password" to "admin",
            ),
            keycloakLoginForm(username = "admin", password = "admin"),
        )
    }

    @Test
    fun signupRequestSerializesToBackendJson() {
        val json = SignUpRequest(
            email = "roman@example.com",
            password = "Password1",
            firstName = "Roman",
            lastName = "Kryvko",
            username = "roman",
        ).toSignUpJson()

        assertEquals(
            """{"email":"roman@example.com","password":"Password1","firstName":"Roman","lastName":"Kryvko","username":"roman"}""",
            json,
        )
    }

    @Test
    fun loginPersistsSessionWithExpiry() = runTest {
        val store = FakeAuthSessionStore()
        val timeProvider = FakeTimeProvider(now = 1_000L)
        val repository = DefaultAuthRepository(
            api = FakeAuthApi(loginResponse = tokenResponse()),
            sessionStore = store,
            timeProvider = timeProvider,
        )

        val session = repository.login("roman", "Password1")

        assertEquals("access", session.accessToken)
        assertEquals("refresh", store.saved?.refreshToken)
        assertEquals(61_000L, store.saved?.expiresAtEpochMillis)
    }

    @Test
    fun expiredSessionRefreshesAndFailedRefreshClearsStore() = runTest {
        val store = FakeAuthSessionStore(
            initial = AuthSession(
                accessToken = "old",
                refreshToken = "refresh",
                expiresAtEpochMillis = 0,
                username = "roman",
            )
        )
        val api = FakeAuthApi(refreshResponse = tokenResponse(accessToken = "new"))
        val repository = DefaultAuthRepository(
            api = api,
            sessionStore = store,
            timeProvider = FakeTimeProvider(now = 1_000L),
        )

        assertNull(repository.currentSession())
        assertEquals("new", repository.refreshSession()?.accessToken)
    }

    @Test
    fun failedRefreshClearsSession() = runTest {
        val store = FakeAuthSessionStore(
            initial = AuthSession(
                accessToken = "old",
                refreshToken = "refresh",
                expiresAtEpochMillis = 0,
                username = "roman",
            )
        )
        val repository = DefaultAuthRepository(
            api = FakeAuthApi(failRefresh = true),
            sessionStore = store,
            timeProvider = FakeTimeProvider(now = 1_000L),
        )

        assertNull(repository.refreshSession())
        assertEquals(true, store.didClear)
    }
}

private class FakeAuthApi(
    private val loginResponse: TokenResponse = tokenResponse(),
    private val refreshResponse: TokenResponse = tokenResponse(),
    private val failRefresh: Boolean = false,
) : AuthApi {
    override suspend fun login(username: String, password: String): TokenResponse = loginResponse

    override suspend fun refresh(refreshToken: String): TokenResponse {
        if (failRefresh) error("refresh failed")
        return refreshResponse
    }

    override suspend fun signUp(request: SignUpRequest) = Unit
}

private class FakeAuthSessionStore(initial: AuthSession? = null) : AuthSessionStore {
    var saved: AuthSession? = initial
    var didClear = false

    override fun get(): AuthSession? = saved

    override fun save(session: AuthSession) {
        saved = session
    }

    override fun clear() {
        saved = null
        didClear = true
    }
}

private class FakeTimeProvider(private val now: Long) : TimeProvider {
    override fun currentTimeMillis(): Long = now
}

private fun tokenResponse(accessToken: String = "access") = TokenResponse(
    accessToken = accessToken,
    refreshToken = "refresh",
    expiresInSeconds = 60,
)
