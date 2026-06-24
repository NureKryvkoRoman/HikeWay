package ua.nure.kryvko.hikeway.data.services.network

import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET
import ua.nure.kryvko.hikeway.data.auth.AuthSessionFactory
import ua.nure.kryvko.hikeway.data.auth.AuthSessionManager
import ua.nure.kryvko.hikeway.data.auth.AuthSessionStore
import ua.nure.kryvko.hikeway.data.auth.JwtDecoder
import ua.nure.kryvko.hikeway.data.services.backend.SignUpRequestDto
import ua.nure.kryvko.hikeway.domain.auth.AuthSession
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

class ApiServicesTest {
    private lateinit var backend: MockWebServer
    private lateinit var keycloak: MockWebServer
    private lateinit var store: TestSessionStore
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var services: ApiServices

    @Before
    fun setUp() {
        backend = MockWebServer()
        keycloak = MockWebServer()
        backend.start()
        keycloak.start()
        store = TestSessionStore()
        val timeProvider = FixedTimeProvider()
        val gson = Gson()
        sessionManager = AuthSessionManager(
            store = store,
            timeProvider = timeProvider,
            currentUserProvider = MutableCurrentUserProvider(),
        )
        services = ApiServices(
            backendBaseUrl = backend.url("/").toString(),
            keycloakBaseUrl = keycloak.url("/").toString(),
            keycloakRealm = "hikeway-keycloak",
            keycloakClientId = "backend-test",
            sessionManager = sessionManager,
            sessionFactory = AuthSessionFactory(timeProvider, JwtDecoder(gson)),
            gson = gson,
        )
    }

    @After
    fun tearDown() {
        backend.shutdown()
        keycloak.shutdown()
    }

    @Test
    fun publicServicesUseGsonAndNeverAttachBearerToken() = runTest {
        sessionManager.save(validSession("secret-access"))
        backend.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        keycloak.enqueue(MockResponse().setResponseCode(200).setBody(tokenJson("login-user")))

        services.backendAuth.signUp(
            SignUpRequestDto(
                email = "roman@example.com",
                password = "Password1",
                firstName = "Roman",
                lastName = "Kryvko",
                username = "roman",
            )
        )
        services.keycloak.login(
            realm = "hikeway-keycloak",
            clientId = "backend-test",
            username = "roman",
            password = "Password1",
        )

        val signupRequest = backend.takeRequest()
        val loginRequest = keycloak.takeRequest()
        assertNull(signupRequest.getHeader("Authorization"))
        assertNull(loginRequest.getHeader("Authorization"))
        assertTrue(signupRequest.body.readUtf8().contains("\"firstName\":\"Roman\""))
        assertTrue(loginRequest.body.readUtf8().contains("grant_type=password"))
    }

    @Test
    fun protectedRequestAddsBearerThenRefreshesAndRetriesOnce() = runTest {
        sessionManager.save(validSession("old-access"))
        backend.enqueue(MockResponse().setResponseCode(401))
        backend.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        keycloak.enqueue(MockResponse().setResponseCode(200).setBody(tokenJson("refreshed-user")))
        val protectedService = services.authenticatedBackend.create(ProtectedService::class.java)

        val body = protectedService.get().use(ResponseBody::string)

        val firstRequest = backend.takeRequest()
        val retriedRequest = backend.takeRequest()
        val refreshRequest = keycloak.takeRequest()
        assertEquals("ok", body)
        assertEquals("Bearer old-access", firstRequest.getHeader("Authorization"))
        assertEquals(
            "Bearer ${store.saved?.accessToken}",
            retriedRequest.getHeader("Authorization"),
        )
        assertTrue(refreshRequest.body.readUtf8().contains("grant_type=refresh_token"))
        assertEquals(2, backend.requestCount)
        assertEquals(1, keycloak.requestCount)
    }

    @Test
    fun failedRefreshClearsSessionAndDoesNotLoop() = runTest {
        sessionManager.save(validSession("old-access"))
        backend.enqueue(MockResponse().setResponseCode(401))
        keycloak.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"invalid_grant"}"""))
        val protectedService = services.authenticatedBackend.create(ProtectedService::class.java)

        val failure = runCatching { protectedService.get() }.exceptionOrNull()

        assertTrue(failure != null)
        assertNull(store.saved)
        assertTrue(store.didClear)
        assertEquals(1, backend.requestCount)
        assertEquals(1, keycloak.requestCount)
    }

    @Test
    fun concurrentUnauthorizedResponsesShareOneRefresh() = runTest {
        sessionManager.save(validSession("old-access"))
        backend.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return if (request.getHeader("Authorization") == "Bearer old-access") {
                    MockResponse().setResponseCode(401)
                } else {
                    MockResponse().setResponseCode(200).setBody("ok")
                }
            }
        }
        keycloak.enqueue(MockResponse().setResponseCode(200).setBody(tokenJson("refreshed-user")))
        val protectedService = services.authenticatedBackend.create(ProtectedService::class.java)

        val results = listOf(
            async(Dispatchers.IO) { protectedService.get().use { it.string() } },
            async(Dispatchers.IO) { protectedService.get().use { it.string() } },
        ).awaitAll()

        assertEquals(listOf("ok", "ok"), results)
        assertEquals(1, keycloak.requestCount)
        assertEquals(4, backend.requestCount)
    }

    @Test
    fun baseUrlsAreNormalizedForRetrofit() {
        assertEquals("http://localhost:8080/", "http://localhost:8080".withTrailingSlash())
        assertFalse("http://localhost:8080/".withTrailingSlash().endsWith("//"))
    }
}

private interface ProtectedService {
    @GET("protected")
    suspend fun get(): ResponseBody
}

private class TestSessionStore : AuthSessionStore {
    var saved: AuthSession? = null
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

private class FixedTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = 1_000L
}

private fun validSession(accessToken: String) = AuthSession(
    accessToken = accessToken,
    refreshToken = "refresh-token",
    expiresAtEpochMillis = Long.MAX_VALUE,
    username = "roman",
    userId = "user-123",
)

private fun tokenJson(userId: String): String {
    return """
        {
          "access_token": "${jwt(userId)}",
          "refresh_token": "new-refresh-token",
          "expires_in": 300
        }
    """.trimIndent()
}

private fun jwt(userId: String): String {
    val encoder = Base64.getUrlEncoder().withoutPadding()
    val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray(StandardCharsets.UTF_8))
    val payload = encoder.encodeToString("""{"sub":"$userId"}""".toByteArray(StandardCharsets.UTF_8))
    return "$header.$payload.signature"
}
