package tech.tamaki.alcoholmeter.ui.screens.history

import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import java.time.LocalDateTime
import tech.tamaki.alcoholmeter.domain.calc.DayStatus
import tech.tamaki.alcoholmeter.ui.screens.summary.SummaryUiState

data class HistorySessionUiState(
    val id: String,
    val monthHeading: String,
    val date: String,
    val weekday: String,
    val timeRange: String,
    val details: String,
    val status: HistorySyncStatus,
)

enum class HistorySyncStatus { Sent, Failed, NotLinked }

data class HistoryListUiState(val sessions: List<HistorySessionUiState>, val loaded: Boolean = true)

data class HistoryDetailUiState(
    val summary: SummaryUiState,
    val startTime: String,
    val endTime: String,
    val edit: DrinkEditUiState?,
)

data class DrinkEditUiState(
    val title: String,
    val selectedPresetId: Long,
    val time: String,
    val date: String,
    val presets: List<DrinkPreset>,
    val sessionStart: LocalDateTime,
    val sessionEnd: LocalDateTime,
    val selectedDateTime: LocalDateTime,
)

val DrinkEditUiState.timeWithinSession: Boolean
    get() = selectedDateTime in sessionStart..sessionEnd
