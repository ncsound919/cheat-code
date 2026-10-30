package com.propslab.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.propslab.PropsLabApp
import java.util.concurrent.TimeUnit

/** Re-runs the prediction gate so stale quotes and stale injury data stop looking usable. */
class FreshnessWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as PropsLabApp
        return try {
            app.repo.expireStalePredictions()
            Result.success()
        } catch (e: Exception) {
            runCatching {
                app.repo.audit(
                    "worker_retry", "freshness",
                    "Freshness check failed (${e::class.java.simpleName}); WorkManager will retry.",
                )
            }
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FreshnessWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("freshness", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
