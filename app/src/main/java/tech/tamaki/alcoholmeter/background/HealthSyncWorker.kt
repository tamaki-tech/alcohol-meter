package tech.tamaki.alcoholmeter.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import tech.tamaki.alcoholmeter.domain.usecase.SyncHealth

@HiltWorker
class HealthSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: SyncHealth,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("sessionId") ?: return Result.failure()
        return if (sync(id)) Result.success() else Result.retry()
    }
}
