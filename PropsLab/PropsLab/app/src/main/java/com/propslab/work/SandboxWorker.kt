package com.propslab.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.propslab.PropsLabApp
import java.util.concurrent.TimeUnit

/**
 * One scheduled turn of the formula loop: gate every formula on a fresh season, promote the
 * best one that clears every check, and let it trade the live board.
 *
 * Nothing here needs a network or a person. The worker only writes through the repository,
 * so the same code path runs from the app, from the button on the sandbox screen, and from
 * WorkManager.
 */
class SandboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PropsLabApp
        return try {
            if (!app.repo.autoRunEnabled()) return Result.success()
            app.repo.runSandboxRound()
            Result.success()
        } catch (e: Exception) {
            // A failed round must not stop the schedule; the next one starts from stored state.
            runCatching {
                app.repo.audit(
                    "worker_retry", PERIODIC_NAME,
                    "Formula round failed (${e::class.java.simpleName}); WorkManager will retry.",
                )
            }
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_NAME = "sandbox-round"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SandboxWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Asks for a turn right now, without disturbing the six-hourly schedule. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<SandboxWorker>().build())
        }
    }
}
