package tech.tamaki.alcoholmeter.domain.model

import java.time.Instant
import java.time.ZoneOffset

enum class Sex { MALE, FEMALE }
enum class HcSyncStatus { NOT_SYNCED, SYNCED, FAILED }
data class UserProfile(
    val weightKg: Double,
    val sex: Sex,
    val eliminationRatePerHour: Double = 0.015,
)
data class DrinkPreset(
    val id: Long,
    val name: String,
    val shortName: String,
    val beverageType: Int,
    val volumeMl: Double,
    val abvPercent: Double,
    val icon: String,
    val colorIndex: Int,
    val sortOrder: Int,
    val showInNotification: Boolean,
)
data class DrinkLog(
    val id: String,
    val sessionId: String,
    val drankAt: Instant,
    val offset: ZoneOffset,
    val name: String,
    val beverageType: Int,
    val volumeMl: Double,
    val abvPercent: Double,
    val pureAlcoholG: Double,
    val colorIndex: Int,
    val hcRecordId: String?,
    val hcVersion: Long = 1,
    val presetId: Long? = null,
)
data class PartySession(
    val id: String,
    val startedAt: Instant,
    val startOffset: ZoneOffset,
    val endedAt: Instant?,
    val endOffset: ZoneOffset?,
    val title: String?,
    val hcSyncStatus: HcSyncStatus,
)
data class SessionWithLogs(
    val session: PartySession,
    val logs: List<DrinkLog>,
)
sealed interface AppState {
    data object BeforeParty : AppState
    data class InParty(
    val data: SessionWithLogs,
) : AppState
    data class Finished(
    val data: SessionWithLogs,
) : AppState
}
enum class HcAvailability { AVAILABLE, NEEDS_UPDATE, UNSUPPORTED }
