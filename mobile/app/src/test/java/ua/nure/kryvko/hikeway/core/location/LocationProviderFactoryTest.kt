package ua.nure.kryvko.hikeway.core.location

import org.junit.Assert.assertSame
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint

class LocationProviderFactoryTest {
    @Test
    fun selectsSimulatedLocationProviderWhenFlagIsEnabled() {
        val simulated = FakeLocationProvider()
        val real = FakeLocationProvider()

        assertSame(
            simulated,
            selectLocationProvider(
                useSimulatedGps = true,
                simulatedProvider = simulated,
                realProvider = real,
            ),
        )
    }

    @Test
    fun selectsRealLocationProviderWhenFlagIsDisabled() {
        val simulated = FakeLocationProvider()
        val real = FakeLocationProvider()

        assertSame(
            real,
            selectLocationProvider(
                useSimulatedGps = false,
                simulatedProvider = simulated,
                realProvider = real,
            ),
        )
    }
}

private class FakeLocationProvider : LocationProvider {
    override suspend fun getCurrentLocation(): GeoPoint {
        return GeoPoint(longitude = 24.0316, latitude = 49.8429)
    }
}
