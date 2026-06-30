package ua.nure.kryvko.hikeway.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeLogDao
import ua.nure.kryvko.hikeway.data.routes.local.RouteDao
import ua.nure.kryvko.hikeway.data.services.backend.SyncService
import ua.nure.kryvko.hikeway.data.sync.RetrofitSyncTransport
import ua.nure.kryvko.hikeway.data.sync.RoomSyncLocalDataSource
import ua.nure.kryvko.hikeway.data.sync.SharedPreferencesSyncMetadataStore
import ua.nure.kryvko.hikeway.data.sync.SyncConflictDao
import ua.nure.kryvko.hikeway.data.sync.SyncCoordinator
import ua.nure.kryvko.hikeway.data.sync.SyncLocalDataSource
import ua.nure.kryvko.hikeway.data.sync.SyncMetadataStore
import ua.nure.kryvko.hikeway.data.sync.SyncTransport
import ua.nure.kryvko.hikeway.data.sync.SyncTrigger
import ua.nure.kryvko.hikeway.domain.auth.CurrentUserProvider

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {
    @Provides
    @Singleton
    fun provideSyncTransport(service: SyncService): SyncTransport = RetrofitSyncTransport(service)

    @Provides
    @Singleton
    fun provideSyncLocalDataSource(
        routeDao: RouteDao,
        hikeLogDao: HikeLogDao,
        conflictDao: SyncConflictDao,
        currentUserProvider: CurrentUserProvider,
    ): SyncLocalDataSource {
        return RoomSyncLocalDataSource(routeDao, hikeLogDao, conflictDao, currentUserProvider)
    }

    @Provides
    @Singleton
    fun provideSyncMetadataStore(@ApplicationContext context: Context): SyncMetadataStore {
        return SharedPreferencesSyncMetadataStore(context)
    }

    @Provides
    @Singleton
    fun provideSyncCoordinator(
        transport: SyncTransport,
        localDataSource: SyncLocalDataSource,
        metadataStore: SyncMetadataStore,
    ): SyncCoordinator = SyncCoordinator(transport, localDataSource, metadataStore)

    @Provides
    @Singleton
    fun provideSyncTrigger(
        coordinator: SyncCoordinator,
        currentUserProvider: CurrentUserProvider,
    ): SyncTrigger = SyncTrigger(coordinator, currentUserProvider)
}
