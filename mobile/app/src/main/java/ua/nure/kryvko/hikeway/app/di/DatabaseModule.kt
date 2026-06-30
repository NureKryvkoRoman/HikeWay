package ua.nure.kryvko.hikeway.app.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeLogDao
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeWayDatabase
import ua.nure.kryvko.hikeway.data.hikelogging.local.MIGRATION_1_2
import ua.nure.kryvko.hikeway.data.hikelogging.local.MIGRATION_2_3
import ua.nure.kryvko.hikeway.data.hikelogging.local.MIGRATION_3_4
import ua.nure.kryvko.hikeway.data.routes.local.RouteDao
import ua.nure.kryvko.hikeway.data.sync.SyncConflictDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HikeWayDatabase {
        return Room.databaseBuilder(
            context,
            HikeWayDatabase::class.java,
            "hikeway.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
    }

    @Provides
    fun provideRouteDao(database: HikeWayDatabase): RouteDao = database.routeDao()

    @Provides
    fun provideHikeLogDao(database: HikeWayDatabase): HikeLogDao = database.hikeLogDao()

    @Provides
    fun provideSyncConflictDao(database: HikeWayDatabase): SyncConflictDao = database.syncConflictDao()
}
