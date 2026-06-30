package ua.nure.kryvko.hikeway.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ua.nure.kryvko.hikeway.BuildConfig
import ua.nure.kryvko.hikeway.core.location.LocationProvider
import ua.nure.kryvko.hikeway.core.location.createLocationProvider
import ua.nure.kryvko.hikeway.data.routepicking.createRouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider {
        return createLocationProvider(
            context = context,
            useSimulatedGps = BuildConfig.USE_SIMULATED_GPS,
        )
    }

    @Provides
    @Singleton
    fun provideRouteTrackingProvider(@ApplicationContext context: Context): RouteTrackingProvider {
        return createRouteTrackingProvider(
            context = context,
            useSimulatedGps = BuildConfig.USE_SIMULATED_GPS,
        )
    }
}
