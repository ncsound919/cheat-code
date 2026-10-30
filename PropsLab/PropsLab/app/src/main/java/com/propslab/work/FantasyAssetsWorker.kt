package com.propslab.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.propslab.PropsLabApp
import java.util.concurrent.TimeUnit

/**
 * Fills the pictures after a capture has already been saved.
 *
 * The roster is usable immediately; this worker only enriches it with ESPN's public athlete
 * directory. It never blocks the save screen, never sends the screenshot, and leaves the
 * initials placeholder in place when a player cannot be matched exactly.
 */
class FantasyAssetsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val leagueId = inputData.getString(KEY_LEAGUE_ID).orEmpty()
        if (leagueId.isBlank()) return Result.failure()
        val app = applicationContext as PropsLabApp
        return try {
            app.repo.enrichFantasyLeague(leagueId)
            Result.success()
        } catch (e: Exception) {
            runCatching {
                app.repo.audit(
                    "worker_retry", NAME,
                    "Fantasy photo lookup failed (${e::class.java.simpleName}); WorkManager will retry.",
                )
            }
            Result.retry()
        }
    }

    companion object {
        const val NAME = "fantasy-assets"
        private const val KEY_LEAGUE_ID = "leagueId"

        fun enqueue(context: Context, leagueId: String) {
            val request = OneTimeWorkRequestBuilder<FantasyAssetsWorker>()
                .setInputData(androidx.work.workDataOf(KEY_LEAGUE_ID to leagueId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "$NAME-$leagueId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
