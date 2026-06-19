package ua.nure.kryvko.hikeway.app

import android.content.Context
import androidx.room.Room
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeWayDatabase
import ua.nure.kryvko.hikeway.data.hikelogging.local.MIGRATION_1_2
import ua.nure.kryvko.hikeway.data.hikelogging.local.RoomHikeLogRepository
import ua.nure.kryvko.hikeway.core.location.LocationProvider
import ua.nure.kryvko.hikeway.core.location.StubLocationProvider
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider
import ua.nure.kryvko.hikeway.data.routes.CompositeRouteRepository
import ua.nure.kryvko.hikeway.data.routes.local.RoomRouteRepository
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.ObserveCompletedHikesUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemTimeProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routes.CustomRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.SaveCustomRouteUseCase
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        HikeWayDatabase::class.java,
        "hikeway.db",
    ).addMigrations(MIGRATION_1_2).build()
    private val locationProvider: LocationProvider = StubLocationProvider()
    private val localRouteRepository = RoomRouteRepository(database.routeDao())
    private val customRouteRepository: CustomRouteRepository = localRouteRepository
    private val routeRepository: RouteRepository = CompositeRouteRepository(
        listOf(StubRouteRepository(), localRouteRepository)
    )
    private val hikeLogRepository: HikeLogRepository = RoomHikeLogRepository(database.hikeLogDao())
    val routeTrackingProvider: RouteTrackingProvider = StubRouteTrackingProvider()
    val timeProvider: TimeProvider = SystemTimeProvider()
    val activeTimer: ActiveTimer = SystemActiveTimer()

    val searchRoutes = SearchRoutesUseCase(routeRepository, locationProvider)
    val saveCompletedHike = SaveCompletedHikeUseCase(hikeLogRepository)
    val observeCompletedHikes = ObserveCompletedHikesUseCase(hikeLogRepository)
    val saveCustomRoute = SaveCustomRouteUseCase(customRouteRepository)
}
