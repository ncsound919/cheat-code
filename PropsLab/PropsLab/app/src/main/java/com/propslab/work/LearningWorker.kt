package com.propslab.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.propslab.PropsLabApp
import java.util.concurrent.TimeUnit

/**
 * The quiet half of autonomy: log the current board, settle finished bets, and refit the
 * calibration, with no screen open.
 *
 * It cannot promote a formula or place a real bet. The six-hour sandbox worker still owns
 * promotion; this one only keeps the model's correction current from whatever results have
 * arrived since the last pass.
 */
class LearningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PropsLabApp
        return try {
            app.repo.learn()
            Result.success()
        } catch (e: Exception) {
            runCatching {
                app.repo.audit(
                    "worker_retry", PERIODIC_NAME,
                    "Learning round failed (${e::class.java.simpleName}); WorkManager will retry.",
                )
            }
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_NAME = "learning-round"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LearningWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
