package tech.tamaki.alcoholmeter.domain.usecase

import javax.inject.Inject

interface HealthSyncOperation {
    /** true は完了、false は再試行が必要。 */
    suspend fun sync(sessionId: String): Boolean
}

class SyncHealth @Inject constructor(private val operation: HealthSyncOperation) {
    suspend operator fun invoke(sessionId: String): Boolean = operation.sync(sessionId)
}

class RequestHealthSync @Inject constructor(private val health: tech.tamaki.alcoholmeter.domain.repository.HealthConnectRepository) {
    operator fun invoke(sessionId: String) = health.enqueueSync(sessionId)
}
