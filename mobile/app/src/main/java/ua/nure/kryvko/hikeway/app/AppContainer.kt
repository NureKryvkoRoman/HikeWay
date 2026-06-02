package ua.nure.kryvko.hikeway.app

import ua.nure.kryvko.hikeway.core.location.LocationProvider
import ua.nure.kryvko.hikeway.core.location.StubLocationProvider
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase

class AppContainer {
    private val locationProvider: LocationProvider = StubLocationProvider()
    private val routeRepository: RouteRepository = StubRouteRepository()

    val searchRoutes = SearchRoutesUseCase(routeRepository, locationProvider)
}
