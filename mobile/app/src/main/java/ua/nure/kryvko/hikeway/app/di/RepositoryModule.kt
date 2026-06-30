package ua.nure.kryvko.hikeway.app.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ua.nure.kryvko.hikeway.BuildConfig
import ua.nure.kryvko.hikeway.data.auth.AuthSessionFactory
import ua.nure.kryvko.hikeway.data.auth.AuthSessionManager
import ua.nure.kryvko.hikeway.data.auth.DefaultAuthRepository
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeLogDao
import ua.nure.kryvko.hikeway.data.hikelogging.local.RoomHikeLogRepository
import ua.nure.kryvko.hikeway.data.pois.remote.RemotePointOfInterestRepository
import ua.nure.kryvko.hikeway.data.routes.CompositeRouteRepository
import ua.nure.kryvko.hikeway.data.routes.local.RoomRouteRepository
import ua.nure.kryvko.hikeway.data.routes.local.RouteDao
import ua.nure.kryvko.hikeway.data.routes.remote.RemoteRouteRepository
import ua.nure.kryvko.hikeway.data.services.backend.BackendAuthService
import ua.nure.kryvko.hikeway.data.services.backend.PoiService
import ua.nure.kryvko.hikeway.data.services.backend.RouteService
import ua.nure.kryvko.hikeway.data.services.keycloak.KeycloakService
import ua.nure.kryvko.hikeway.data.services.network.TokenRefreshCoordinator
import ua.nure.kryvko.hikeway.data.sync.SyncTrigger
import ua.nure.kryvko.hikeway.domain.auth.AuthRepository
import ua.nure.kryvko.hikeway.domain.auth.CurrentUserProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository
import ua.nure.kryvko.hikeway.domain.routes.CustomRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAuthRepository(
        keycloakService: KeycloakService,
        backendAuthService: BackendAuthService,
        sessionManager: AuthSessionManager,
        sessionFactory: AuthSessionFactory,
        refreshCoordinator: TokenRefreshCoordinator,
        gson: Gson,
        syncTrigger: SyncTrigger,
    ): AuthRepository {
        return DefaultAuthRepository(
            keycloakService = keycloakService,
            backendAuthService = backendAuthService,
            keycloakRealm = BuildConfig.KEYCLOAK_REALM,
            keycloakClientId = BuildConfig.KEYCLOAK_CLIENT_ID,
            sessionManager = sessionManager,
            sessionFactory = sessionFactory,
            refreshCoordinator = refreshCoordinator,
            gson = gson,
            onAuthenticated = syncTrigger::invoke,
        )
    }

    @Provides
    @Singleton
    fun provideLocalRouteRepository(
        routeDao: RouteDao,
        currentUserProvider: CurrentUserProvider,
        syncTrigger: SyncTrigger,
    ): RoomRouteRepository {
        return RoomRouteRepository(
            dao = routeDao,
            currentUserProvider = currentUserProvider,
            onLocalMutation = syncTrigger::invoke,
        )
    }

    @Provides
    @Singleton
    fun provideCustomRouteRepository(localRepository: RoomRouteRepository): CustomRouteRepository {
        return localRepository
    }

    @Provides
    @Singleton
    fun provideRemoteRouteRepository(
        routeService: RouteService,
        gson: Gson,
    ): RemoteRouteRepository = RemoteRouteRepository(routeService, gson)

    @Provides
    @Singleton
    fun provideRouteRepository(
        remoteRepository: RemoteRouteRepository,
        localRepository: RoomRouteRepository,
    ): RouteRepository {
        return CompositeRouteRepository(listOf(remoteRepository, localRepository))
    }

    @Provides
    @Singleton
    fun providePointOfInterestRepository(
        poiService: PoiService,
        gson: Gson,
    ): PointOfInterestRepository = RemotePointOfInterestRepository(poiService, gson)

    @Provides
    @Singleton
    fun provideHikeLogRepository(
        hikeLogDao: HikeLogDao,
        routeDao: RouteDao,
        currentUserProvider: CurrentUserProvider,
        syncTrigger: SyncTrigger,
    ): HikeLogRepository {
        return RoomHikeLogRepository(
            dao = hikeLogDao,
            currentUserProvider = currentUserProvider,
            onLocalMutation = syncTrigger::invoke,
            routeDao = routeDao,
        )
    }
}
