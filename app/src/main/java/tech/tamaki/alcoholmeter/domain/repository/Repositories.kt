package tech.tamaki.alcoholmeter.domain.repository

import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import tech.tamaki.alcoholmeter.domain.model.AppState
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.domain.model.HcAvailability
import tech.tamaki.alcoholmeter.domain.model.SessionWithLogs
import tech.tamaki.alcoholmeter.domain.model.UserProfile

interface PartyRepository {
    fun observeAppState(): Flow<AppState>
    fun observeSession(id: String): Flow<SessionWithLogs?>
    fun observeHistory(): Flow<List<SessionWithLogs>>
    fun observeAllLogs(): Flow<List<DrinkLog>>
    suspend fun startParty()
    suspend fun addDrink(presetId: Long): DrinkLog
    suspend fun deleteDrink(logId: String)
    suspend fun updateDrink(logId: String, presetId: Long?, drankAt: Instant?)
    suspend fun endParty(endAt: Instant)
    suspend fun closeSummary()
    suspend fun deleteSession(id: String)
}
interface PresetRepository {
    fun observePresets(): Flow<List<DrinkPreset>>
    suspend fun upsert(p: DrinkPreset)
    suspend fun reorder(idsInOrder: List<Long>)
    suspend fun delete(id: Long)
}
interface SettingsRepository {
    val profile: Flow<UserProfile?>
    val onboardingCompleted: Flow<Boolean>
    val recordStartDate: Flow<LocalDate?>
    suspend fun saveProfile(p: UserProfile)
    suspend fun completeOnboarding()
}
interface HealthConnectRepository {
    suspend fun availability(): HcAvailability
    suspend fun hasPermissions(): Boolean
    suspend fun hasWritePermission(): Boolean = hasPermissions()
    val requiredPermissions: Set<String>
    suspend fun latestWeight(): Pair<Double, Instant>?
    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>>
    fun enqueueSync(sessionId: String)
    fun enqueueRecovery(sessionId: String)
}
