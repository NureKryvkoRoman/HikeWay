package ua.nure.kryvko.hikeway.app.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ua.nure.kryvko.hikeway.domain.auth.CurrentUserProvider
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemTimeProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider()

    @Provides
    @Singleton
    fun provideActiveTimer(): ActiveTimer = SystemActiveTimer()

    @Provides
    @Singleton
    fun provideMutableCurrentUserProvider(): MutableCurrentUserProvider = MutableCurrentUserProvider()

    @Provides
    fun provideCurrentUserProvider(provider: MutableCurrentUserProvider): CurrentUserProvider = provider
}
