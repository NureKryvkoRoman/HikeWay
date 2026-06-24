package ua.nure.kryvko.hikeway.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import ua.nure.kryvko.hikeway.app.AppContainer

class SyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val container = AppContainer(applicationContext)
        return try {
            if (container.restoreSession() == null) {
                Result.success()
            } else {
                runCatching {
                    container.synchronizeNow()
                    Result.success()
                }.getOrElse {
                    Result.retry()
                }
            }
        } finally {
            container.close()
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
