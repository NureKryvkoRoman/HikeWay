package ua.nure.kryvko.hikeway.app

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import ua.nure.kryvko.hikeway.BuildConfig
import ua.nure.kryvko.hikeway.data.auth.AuthSessionFactory
import ua.nure.kryvko.hikeway.data.auth.AuthSessionManager
import ua.nure.kryvko.hikeway.data.auth.DefaultAuthRepository
import ua.nure.kryvko.hikeway.data.auth.JwtDecoder
import ua.nure.kryvko.hikeway.data.auth.SharedPreferencesAuthSessionStore
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeWayDatabase
import ua.nure.kryvko.hikeway.data.pois.stub.StubPointOfInterestRepository
import ua.nure.kryvko.hikeway.data.hikelogging.local.MIGRATION_1_2
import ua.nure.kryvko.hikeway.data.hikelogging.local.MIGRATION_2_3
import ua.nure.kryvko.hikeway.data.hikelogging.local.RoomHikeLogRepository
import ua.nure.kryvko.hikeway.core.location.LocationProvider
import ua.nure.kryvko.hikeway.core.location.createLocationProvider
import ua.nure.kryvko.hikeway.data.routepicking.createRouteTrackingProvider
import ua.nure.kryvko.hikeway.data.routes.CompositeRouteRepository
import ua.nure.kryvko.hikeway.data.routes.local.RoomRouteRepository
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
import ua.nure.kryvko.hikeway.data.services.network.ApiServices
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.ObserveCompletedHikesUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemTimeProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider
import ua.nure.kryvko.hikeway.domain.auth.AuthRepository
import ua.nure.kryvko.hikeway.domain.auth.LoginUseCase
import ua.nure.kryvko.hikeway.domain.auth.LogoutUseCase
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.auth.ObserveAuthSessionUseCase
import ua.nure.kryvko.hikeway.domain.auth.RestoreSessionUseCase
import ua.nure.kryvko.hikeway.domain.auth.SignUpUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routes.CustomRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.GetCurrentLocationUseCase
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.SaveCustomRouteUseCase
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        HikeWayDatabase::class.java,
        "hikeway.db",
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    private val locationProvider: LocationProvider = createLocationProvider(
        context = context,
        useSimulatedGps = BuildConfig.USE_SIMULATED_GPS,
    )
    val routeTrackingProvider: RouteTrackingProvider = createRouteTrackingProvider(
        context = context,
        useSimulatedGps = BuildConfig.USE_SIMULATED_GPS,
    )
    val timeProvider: TimeProvider = SystemTimeProvider()
    val activeTimer: ActiveTimer = SystemActiveTimer()
    private val currentUserProvider = MutableCurrentUserProvider()
    private val gson = Gson()
    private val authSessionManager = AuthSessionManager(
        store = SharedPreferencesAuthSessionStore(context),
        timeProvider = timeProvider,
        currentUserProvider = currentUserProvider,
    )
    private val authSessionFactory = AuthSessionFactory(
        timeProvider = timeProvider,
        jwtDecoder = JwtDecoder(gson),
    )
    private val apiServices = ApiServices(
        backendBaseUrl = BuildConfig.BACKEND_BASE_URL,
        keycloakBaseUrl = BuildConfig.KEYCLOAK_BASE_URL,
        keycloakRealm = BuildConfig.KEYCLOAK_REALM,
        keycloakClientId = BuildConfig.KEYCLOAK_CLIENT_ID,
        sessionManager = authSessionManager,
        sessionFactory = authSessionFactory,
        gson = gson,
    )
    private val authRepository: AuthRepository = DefaultAuthRepository(
        keycloakService = apiServices.keycloak,
        backendAuthService = apiServices.backendAuth,
        keycloakRealm = BuildConfig.KEYCLOAK_REALM,
        keycloakClientId = BuildConfig.KEYCLOAK_CLIENT_ID,
        sessionManager = authSessionManager,
        sessionFactory = authSessionFactory,
        refreshCoordinator = apiServices.tokenRefreshCoordinator,
        gson = gson,
    )
    private val localRouteRepository = RoomRouteRepository(database.routeDao(), currentUserProvider)
    private val customRouteRepository: CustomRouteRepository = localRouteRepository
    private val routeRepository: RouteRepository = CompositeRouteRepository(
        listOf(StubRouteRepository(), localRouteRepository)
    )
    private val pointOfInterestRepository: PointOfInterestRepository =
        StubPointOfInterestRepository()
    private val hikeLogRepository: HikeLogRepository = RoomHikeLogRepository(
        database.hikeLogDao(),
        currentUserProvider,
    )

    val searchRoutes = SearchRoutesUseCase(routeRepository, locationProvider)
    val getCurrentLocation = GetCurrentLocationUseCase(locationProvider)
    val saveCompletedHike = SaveCompletedHikeUseCase(hikeLogRepository)
    val observeCompletedHikes = ObserveCompletedHikesUseCase(hikeLogRepository)
    val saveCustomRoute = SaveCustomRouteUseCase(customRouteRepository)
    val getPointsOfInterest = GetPointsOfInterestUseCase(pointOfInterestRepository)
    val ratePointOfInterest = RatePointOfInterestUseCase(pointOfInterestRepository)
    val login = LoginUseCase(authRepository)
    val signUp = SignUpUseCase(authRepository)
    val restoreSession = RestoreSessionUseCase(authRepository)
    val logout = LogoutUseCase(authRepository)
    val observeAuthSession = ObserveAuthSessionUseCase(authRepository)
}
