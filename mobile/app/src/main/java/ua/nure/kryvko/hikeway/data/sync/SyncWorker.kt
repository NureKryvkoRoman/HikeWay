package ua.nure.kryvko.hikeway.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import ua.nure.kryvko.hikeway.domain.auth.RestoreSessionUseCase

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val restoreSession: RestoreSessionUseCase,
    private val syncTrigger: SyncTrigger,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        return if (restoreSession() == null) {
            Result.success()
        } else {
            runCatching {
                syncTrigger()
                Result.success()
            }.getOrElse {
                Result.retry()
            }
        }
    }
}

object SyncWorkScheduler {
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private const val PERIODIC_SYNC_WORK = "hikeway-periodic-sync"
}
