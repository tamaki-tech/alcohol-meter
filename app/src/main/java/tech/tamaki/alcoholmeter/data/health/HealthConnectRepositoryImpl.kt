package tech.tamaki.alcoholmeter.data.health

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.RecordIdFilter
import android.health.connect.datatypes.AlcoholConsumptionRecord
import android.health.connect.datatypes.Metadata
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Volume
import android.os.Build
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.BackoffPolicy
import java.util.concurrent.TimeUnit
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import tech.tamaki.alcoholmeter.background.HealthSyncWorker
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.HcAvailability
import tech.tamaki.alcoholmeter.domain.repository.HealthConnectRepository

@Singleton
class HealthConnectRepositoryImpl @Inject constructor(@param:ApplicationContext private val context: Context) : HealthConnectRepository, HealthSyncGateway {
    override val requiredPermissions = setOf("android.permission.health.WRITE_ALCOHOL_CONSUMPTION", "android.permission.health.READ_WEIGHT")
    override suspend fun availability(): HcAvailability {
        if (Build.VERSION.SDK_INT < 37 && SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) < 21) return HcAvailability.NEEDS_UPDATE
        return if (runCatching { Class.forName("android.health.connect.datatypes.AlcoholConsumptionRecord"); context.getSystemService(HealthConnectManager::class.java) }.getOrNull() != null) HcAvailability.AVAILABLE else HcAvailability.UNSUPPORTED
    }
    override suspend fun hasPermissions() = requiredPermissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    override suspend fun hasWritePermission() = canWriteAlcohol()
    override suspend fun canWriteAlcohol() = context.checkSelfPermission("android.permission.health.WRITE_ALCOHOL_CONSUMPTION") == PackageManager.PERMISSION_GRANTED
    override fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> = object : ActivityResultContract<Set<String>, Set<String>>() {
        private val delegate = ActivityResultContracts.RequestMultiplePermissions()
        override fun createIntent(context: Context, input: Set<String>): Intent = delegate.createIntent(context, input.toTypedArray())
        override fun parseResult(resultCode: Int, intent: Intent?): Set<String> = delegate.parseResult(resultCode, intent).filterValues { it }.keys
    }
    override suspend fun latestWeight(): Pair<Double, Instant>? {
        val request = ReadRecordsRequestUsingFilters.Builder(WeightRecord::class.java).setAscending(false).setPageSize(1).build()
        return call<android.health.connect.ReadRecordsResponse<WeightRecord>> { receiver -> manager().readRecords(request, context.mainExecutor, receiver) }.records.firstOrNull()?.let { it.weight.inGrams / 1000.0 to it.time }
    }
    override fun enqueueSync(sessionId: String) {
        val request = OneTimeWorkRequestBuilder<HealthSyncWorker>().setInputData(workDataOf("sessionId" to sessionId)).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("health-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
    override fun enqueueRecovery(sessionId: String) {
        val request = OneTimeWorkRequestBuilder<HealthSyncWorker>().setInputData(workDataOf("sessionId" to sessionId)).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("health-sync-recovery-$sessionId", ExistingWorkPolicy.KEEP, request)
    }
    private fun manager() = checkNotNull(context.getSystemService(HealthConnectManager::class.java))
    // availability は API 37 または U Extensions 21 とクラスの存在を検査する。
    @SuppressLint("NewApi")
    override suspend fun insert(logs: List<DrinkLog>): Map<String, String> {
        check(availability() == HcAvailability.AVAILABLE)
        if (logs.isEmpty()) return emptyMap()
        val records: List<Record> = logs.map { log ->
            val data = log.toAlcoholRecordData()
            val metadata = Metadata.Builder().setClientRecordId(data.clientRecordId).setClientRecordVersion(log.hcVersion).setRecordingMethod(Metadata.RECORDING_METHOD_MANUAL_ENTRY).apply { log.hcRecordId?.let { setId(it) } }.build()
            AlcoholConsumptionRecord.Builder(metadata, data.time, data.beverageType).setStartZoneOffset(data.offset).setEndZoneOffset(data.offset).setServingVolume(Volume.fromLiters(data.servingVolumeLiters)).setAlcoholByVolume(Percentage.fromValue(data.alcoholByVolumePercent)).setNotes(data.notes).build()
        }
        val newRecords = records.filter { it.metadata.id.isNullOrEmpty() }
        val existing = records.filterNot { it.metadata.id.isNullOrEmpty() }
        if (existing.isNotEmpty()) call<Void?> { receiver -> manager().updateRecords(existing, context.mainExecutor, receiver) }
        val inserted = if (newRecords.isEmpty()) emptyList() else call<android.health.connect.InsertRecordsResponse> { receiver -> manager().insertRecords(newRecords, context.mainExecutor, receiver) }.records
        return (inserted + existing).associate { checkNotNull(it.metadata.clientRecordId) to it.metadata.id }
    }
    @SuppressLint("NewApi")
    override suspend fun delete(ids: List<String>) {
        check(availability() == HcAvailability.AVAILABLE)
        if (ids.isNotEmpty()) call<Void?> { receiver -> manager().deleteRecords(ids.map { RecordIdFilter.fromClientRecordId(AlcoholConsumptionRecord::class.java, it) }, context.mainExecutor, receiver) }
    }
    private suspend fun <T> call(block: (OutcomeReceiver<T, HealthConnectException>) -> Unit): T = suspendCancellableCoroutine { cont ->
        block(object : OutcomeReceiver<T, HealthConnectException> {
            override fun onResult(result: T) { if (cont.isActive) cont.resume(result) }
            override fun onError(error: HealthConnectException) { if (cont.isActive) cont.resumeWithException(error) }
        })
    }
}
