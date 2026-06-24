package ua.nure.kryvko.hikeway.data.auth

import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ua.nure.kryvko.hikeway.data.services.backend.BackendAuthService
import ua.nure.kryvko.hikeway.data.services.backend.SignUpRequestDto
import ua.nure.kryvko.hikeway.data.services.backend.toDto
import ua.nure.kryvko.hikeway.data.services.keycloak.KeycloakService
import ua.nure.kryvko.hikeway.data.services.keycloak.TokenResponseDto
import ua.nure.kryvko.hikeway.data.services.network.TokenRefreshCoordinator
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.auth.SignUpRequest
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

class AuthDataTest {
    @Test
    fun signupRequestMapsToBackendDto() {
        val dto = SignUpRequest(
            email = "roman@example.com",
            password = "Password1",
            firstName = "Roman",
            lastName = "Kryvko",
            username = "roman",
        ).toDto()

        assertEquals("roman@example.com", dto.email)
        assertEquals("Roman", dto.firstName)
        assertEquals("roman", dto.username)
    }

    @Test
    fun loginPersistsSessionWithExpiry() = runTest {
        val fixture = AuthFixture()

        val session = fixture.repository.login("roman", "Password1")

        assertEquals("user-123", session.userId)
        assertEquals("refresh", fixture.store.saved?.refreshToken)
        assertEquals(61_000L, fixture.store.saved?.expiresAtEpochMillis)
        assertEquals("user-123", fixture.currentUserProvider.currentUserId.value)
        assertEquals("roman", fixture.keycloak.lastLoginUsername)
    }

    @Test
    fun extractsSubjectFromJwtUsingGson() {
        assertEquals(
            "keycloak-user-id",
            JwtDecoder(Gson()).subject(jwt(userId = "keycloak-user-id")),
        )
    }

    @Test
    fun expiredSessionRefreshesAndActivatesNewUser() = runTest {
        val fixture = AuthFixture(
            initialSession = expiredSession(),
            refreshResponse = tokenResponse(userId = "new-user"),
        )

        assertNull(fixture.repository.currentSession())
        assertEquals("new-user", fixture.repository.refreshSession()?.userId)
        assertEquals("new-user", fixture.currentUserProvider.currentUserId.value)
    }

    @Test
    fun failedRefreshClearsSession() = runTest {
        val fixture = AuthFixture(
            initialSession = expiredSession(),
            failRefresh = true,
        )

        assertNull(fixture.repository.refreshSession())
        assertEquals(true, fixture.store.didClear)
        assertNull(fixture.currentUserProvider.currentUserId.value)
    }
}

private class AuthFixture(
    initialSession: AuthSession? = null,
    refreshResponse: TokenResponseDto = tokenResponse(),
    failRefresh: Boolean = false,
) {
    val store = FakeAuthSessionStore(initialSession)
    val currentUserProvider = MutableCurrentUserProvider()
    val keycloak = FakeKeycloakService(
        loginResponse = tokenResponse(),
        refreshResponse = refreshResponse,
        failRefresh = failRefresh,
    )
    private val backend = FakeBackendAuthService()
    private val gson = Gson()
    private val timeProvider = FakeTimeProvider(now = 1_000L)
    private val sessionManager = AuthSessionManager(store, timeProvider, currentUserProvider)
    private val sessionFactory = AuthSessionFactory(timeProvider, JwtDecoder(gson))
    private val refreshCoordinator = TokenRefreshCoordinator(
        keycloakService = keycloak,
        realm = "hikeway-keycloak",
        clientId = "backend-test",
        sessionManager = sessionManager,
        sessionFactory = sessionFactory,
    )
    val repository = DefaultAuthRepository(
        keycloakService = keycloak,
        backendAuthService = backend,
        keycloakRealm = "hikeway-keycloak",
        keycloakClientId = "backend-test",
        sessionManager = sessionManager,
        sessionFactory = sessionFactory,
        refreshCoordinator = refreshCoordinator,
        gson = gson,
    )
}

private class FakeKeycloakService(
    private val loginResponse: TokenResponseDto,
    private val refreshResponse: TokenResponseDto,
    private val failRefresh: Boolean,
) : KeycloakService {
    var lastLoginUsername: String? = null

    override suspend fun login(
        realm: String,
        clientId: String,
        grantType: String,
        username: String,
        password: String,
    ): TokenResponseDto {
        lastLoginUsername = username
        return loginResponse
    }

    override suspend fun refresh(
        realm: String,
        clientId: String,
        grantType: String,
        refreshToken: String,
    ): TokenResponseDto {
        if (failRefresh) error("refresh failed")
        return refreshResponse
    }
}

private class FakeBackendAuthService : BackendAuthService {
    override suspend fun signUp(request: SignUpRequestDto) = Unit
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

private fun expiredSession() = AuthSession(
    accessToken = "old",
    refreshToken = "refresh",
    expiresAtEpochMillis = 0,
    username = "roman",
    userId = "old-user",
)

private fun tokenResponse(userId: String = "user-123") = TokenResponseDto(
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
