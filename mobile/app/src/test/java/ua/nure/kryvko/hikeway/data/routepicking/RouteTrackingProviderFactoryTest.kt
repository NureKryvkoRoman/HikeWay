package ua.nure.kryvko.hikeway.data.routepicking

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertSame
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.routepicking.RouteProgress
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider

class RouteTrackingProviderFactoryTest {
    @Test
    fun selectsSimulatedProviderWhenFlagIsEnabled() {
        val simulated = FakeRouteTrackingProvider()
        val real = FakeRouteTrackingProvider()

        assertSame(
            simulated,
            selectRouteTrackingProvider(
                useSimulatedGps = true,
                simulatedProvider = simulated,
                realProvider = real,
            ),
        )
    }

    @Test
    fun selectsRealProviderWhenFlagIsDisabled() {
        val simulated = FakeRouteTrackingProvider()
        val real = FakeRouteTrackingProvider()

        assertSame(
            real,
            selectRouteTrackingProvider(
                useSimulatedGps = false,
                simulatedProvider = simulated,
                realProvider = real,
            ),
        )
    }
}

private class FakeRouteTrackingProvider : RouteTrackingProvider {
    override fun positions(route: Route, startIndex: Int): Flow<RouteProgress> = emptyFlow()
}
