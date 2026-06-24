package ua.nure.kryvko.hikeway.data.services.network

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import ua.nure.kryvko.hikeway.data.auth.AuthSessionFactory
import ua.nure.kryvko.hikeway.data.auth.AuthSessionManager
import ua.nure.kryvko.hikeway.data.services.backend.BackendAuthService
import ua.nure.kryvko.hikeway.data.services.keycloak.KeycloakService

class ApiServices(
    backendBaseUrl: String,
    keycloakBaseUrl: String,
    keycloakRealm: String,
    keycloakClientId: String,
    sessionManager: AuthSessionManager,
    sessionFactory: AuthSessionFactory,
    val gson: Gson = Gson(),
) {
    private val keycloakRetrofit = RetrofitFactory.create(keycloakBaseUrl, gson)
    val keycloak: KeycloakService = keycloakRetrofit.create(KeycloakService::class.java)

    private val publicBackendRetrofit = RetrofitFactory.create(backendBaseUrl, gson)
    val backendAuth: BackendAuthService =
        publicBackendRetrofit.create(BackendAuthService::class.java)

    val tokenRefreshCoordinator = TokenRefreshCoordinator(
        keycloakService = keycloak,
        realm = keycloakRealm,
        clientId = keycloakClientId,
        sessionManager = sessionManager,
        sessionFactory = sessionFactory,
    )

    val authenticatedBackend: Retrofit = RetrofitFactory.create(
        baseUrl = backendBaseUrl,
        gson = gson,
        client = OkHttpClient.Builder()
            .addInterceptor(BearerAuthInterceptor(sessionManager))
            .authenticator(AccessTokenAuthenticator(tokenRefreshCoordinator))
            .build(),
    )
}
