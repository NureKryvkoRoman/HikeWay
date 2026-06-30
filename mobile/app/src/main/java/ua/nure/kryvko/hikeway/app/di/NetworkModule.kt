package ua.nure.kryvko.hikeway.app.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ua.nure.kryvko.hikeway.BuildConfig
import ua.nure.kryvko.hikeway.data.auth.AuthSessionFactory
import ua.nure.kryvko.hikeway.data.auth.AuthSessionManager
import ua.nure.kryvko.hikeway.data.auth.AuthSessionStore
import ua.nure.kryvko.hikeway.data.auth.JwtDecoder
import ua.nure.kryvko.hikeway.data.auth.SharedPreferencesAuthSessionStore
import ua.nure.kryvko.hikeway.data.services.backend.BackendAuthService
import ua.nure.kryvko.hikeway.data.services.backend.PoiService
import ua.nure.kryvko.hikeway.data.services.backend.RouteService
import ua.nure.kryvko.hikeway.data.services.backend.SyncService
import ua.nure.kryvko.hikeway.data.services.keycloak.KeycloakService
import ua.nure.kryvko.hikeway.data.services.network.ApiServices
import ua.nure.kryvko.hikeway.data.services.network.TokenRefreshCoordinator
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideAuthSessionStore(@ApplicationContext context: Context): AuthSessionStore {
        return SharedPreferencesAuthSessionStore(context)
    }

    @Provides
    @Singleton
    fun provideJwtDecoder(gson: Gson): JwtDecoder = JwtDecoder(gson)

    @Provides
    @Singleton
    fun provideAuthSessionFactory(
        timeProvider: TimeProvider,
        jwtDecoder: JwtDecoder,
    ): AuthSessionFactory = AuthSessionFactory(timeProvider, jwtDecoder)

    @Provides
    @Singleton
    fun provideAuthSessionManager(
        store: AuthSessionStore,
        timeProvider: TimeProvider,
        currentUserProvider: MutableCurrentUserProvider,
    ): AuthSessionManager {
        return AuthSessionManager(store, timeProvider, currentUserProvider)
    }

    @Provides
    @Singleton
    fun provideApiServices(
        sessionManager: AuthSessionManager,
        sessionFactory: AuthSessionFactory,
        gson: Gson,
    ): ApiServices {
        return ApiServices(
            backendBaseUrl = BuildConfig.BACKEND_BASE_URL,
            keycloakBaseUrl = BuildConfig.KEYCLOAK_BASE_URL,
            keycloakRealm = BuildConfig.KEYCLOAK_REALM,
            keycloakClientId = BuildConfig.KEYCLOAK_CLIENT_ID,
            sessionManager = sessionManager,
            sessionFactory = sessionFactory,
            gson = gson,
        )
    }

    @Provides
    fun provideKeycloakService(apiServices: ApiServices): KeycloakService = apiServices.keycloak

    @Provides
    fun provideBackendAuthService(apiServices: ApiServices): BackendAuthService = apiServices.backendAuth

    @Provides
    fun provideRouteService(apiServices: ApiServices): RouteService = apiServices.routes

    @Provides
    fun providePoiService(apiServices: ApiServices): PoiService = apiServices.pois

    @Provides
    fun provideSyncService(apiServices: ApiServices): SyncService = apiServices.sync

    @Provides
    fun provideTokenRefreshCoordinator(apiServices: ApiServices): TokenRefreshCoordinator {
        return apiServices.tokenRefreshCoordinator
    }
}
