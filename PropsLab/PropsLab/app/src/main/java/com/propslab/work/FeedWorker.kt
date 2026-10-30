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
 * The half of the loop that runs on the world instead of on a simulation.
 *
 * Every half hour it reads whatever box scores and prices the two providers will give it,
 * settles the live book against the finished games, and lets the promoted formula take
 * picks on the fresh board. It never promotes a formula: that is the sandbox round's job,
 * and keeping the two separate is what stops a good week at the book from looking like a
 * discovered edge.
 *
 * With no keys saved it writes a note and returns success — the schedule is not an error.
 */
class FeedWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as PropsLabApp
        return try {
            val report = app.repo.refreshFeed()
            val auto = app.repo.autoRunEnabled()
            app.repo.refreshLiveBook(place = auto)
            // New quotes and finished games are exactly what the ledger learns from. Keep
            // it current on the same pass instead of waiting for the slower learning worker.
            if (report.quotes > 0 || report.settled > 0) app.repo.learn()
            Result.success()
        } catch (e: Exception) {
            // The next run starts from stored state; a failed sync must not break the schedule.
            // Keep the reason in the audit trail instead of discarding the exception silently.
            runCatching {
                app.repo.audit(
                    "worker_retry", PERIODIC_NAME,
                    "Feed sync failed (${e::class.java.simpleName}); WorkManager will retry.",
                )
            }
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_NAME = "feed-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FeedWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Asks for a sync now, without disturbing the schedule. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<FeedWorker>().build())
        }
    }
}
