package com.shelfie.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shelfie.app.data.sync.SyncManager
import com.shelfie.app.domain.model.SyncException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs one [SyncManager.sync]; scheduled by [SyncScheduler]. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        syncManager.sync().fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                when {
                    // Signed out by the sync itself; nothing left to retry.
                    error is SyncException.SessionExpired -> Result.success()
                    runAttemptCount >= MAX_RETRIES -> Result.failure()
                    else -> Result.retry()
                }
            },
        )

    private companion object {
        const val MAX_RETRIES = 5
    }
}
