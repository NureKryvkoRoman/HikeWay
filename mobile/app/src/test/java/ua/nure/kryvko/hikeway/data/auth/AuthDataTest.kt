package ua.nure.kryvko.hikeway.data.auth

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
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
        val currentUserProvider = MutableCurrentUserProvider()
        val repository = DefaultAuthRepository(
            api = FakeAuthApi(loginResponse = tokenResponse(userId = "user-123")),
            sessionStore = store,
            timeProvider = timeProvider,
            currentUserProvider = currentUserProvider,
        )

        val session = repository.login("roman", "Password1")

        assertEquals("user-123", session.userId)
        assertEquals("refresh", store.saved?.refreshToken)
        assertEquals(61_000L, store.saved?.expiresAtEpochMillis)
        assertEquals("user-123", currentUserProvider.currentUserId.value)
    }

    @Test
    fun extractsSubjectFromJwt() {
        assertEquals("keycloak-user-id", extractJwtSubject(jwt(userId = "keycloak-user-id")))
    }

    @Test
    fun expiredSessionRefreshesAndFailedRefreshClearsStore() = runTest {
        val currentUserProvider = MutableCurrentUserProvider()
        val store = FakeAuthSessionStore(
            initial = AuthSession(
                accessToken = "old",
                refreshToken = "refresh",
                expiresAtEpochMillis = 0,
                username = "roman",
                userId = "old-user",
            )
        )
        val api = FakeAuthApi(refreshResponse = tokenResponse(userId = "new-user"))
        val repository = DefaultAuthRepository(
            api = api,
            sessionStore = store,
            timeProvider = FakeTimeProvider(now = 1_000L),
            currentUserProvider = currentUserProvider,
        )

        assertNull(repository.currentSession())
        assertEquals("new-user", repository.refreshSession()?.userId)
        assertEquals("new-user", currentUserProvider.currentUserId.value)
    }

    @Test
    fun failedRefreshClearsSession() = runTest {
        val currentUserProvider = MutableCurrentUserProvider()
        val store = FakeAuthSessionStore(
            initial = AuthSession(
                accessToken = "old",
                refreshToken = "refresh",
                expiresAtEpochMillis = 0,
                username = "roman",
                userId = "old-user",
            )
        )
        val repository = DefaultAuthRepository(
            api = FakeAuthApi(failRefresh = true),
            sessionStore = store,
            timeProvider = FakeTimeProvider(now = 1_000L),
            currentUserProvider = currentUserProvider,
        )

        assertNull(repository.refreshSession())
        assertEquals(true, store.didClear)
        assertNull(currentUserProvider.currentUserId.value)
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

private fun tokenResponse(userId: String = "user-123") = TokenResponse(
    accessToken = jwt(userId),
    refreshToken = "refresh",
    expiresInSeconds = 60,
)

private fun jwt(userId: String): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray(StandardCharsets.UTF_8))
    val payload = encoder.encodeToString("""{"sub":"$userId"}""".toByteArray(StandardCharsets.UTF_8))
    return "$header.$payload.signature"
}
