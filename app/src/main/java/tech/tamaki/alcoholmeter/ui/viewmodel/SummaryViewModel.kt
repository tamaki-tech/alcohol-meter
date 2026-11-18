package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.tamaki.alcoholmeter.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.calc.BacCalculator
import tech.tamaki.alcoholmeter.domain.model.HcSyncStatus
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.SessionWithLogs
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import tech.tamaki.alcoholmeter.domain.usecase.RequestHealthSync
import tech.tamaki.alcoholmeter.domain.usecase.HealthPermissions
import tech.tamaki.alcoholmeter.domain.usecase.CloseSummary
import tech.tamaki.alcoholmeter.domain.usecase.ObservePartyData
import tech.tamaki.alcoholmeter.domain.usecase.ObserveSettings
import tech.tamaki.alcoholmeter.ui.format.formatBac
import tech.tamaki.alcoholmeter.ui.format.formatDate
import tech.tamaki.alcoholmeter.ui.format.formatGrams
import tech.tamaki.alcoholmeter.ui.format.formatTime
import tech.tamaki.alcoholmeter.ui.screens.summary.BacPointUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.DrinkCategoryUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.DrinkRowUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.HealthSyncUiStatus
import tech.tamaki.alcoholmeter.ui.screens.summary.SessionMetricUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.SummaryUiState

fun summaryState(session: SessionWithLogs, profile: UserProfile?, clock: Clock, context: Context, allLogs: List<DrinkLog>): SummaryUiState {
    val data = session.session
    val logs = session.logs.sortedBy { it.drankAt }
    val last = logs.lastOrNull()?.drankAt
    val relevant = if (last == null) emptyList() else allLogs.filter { !it.drankAt.isAfter(last) }
    val first = logs.firstOrNull()?.drankAt
    val series = if (profile == null || first == null || last == null) emptyList() else {
        val sober = BacCalculator.soberAt(relevant, profile, last) ?: last
        val points = mutableListOf<Pair<java.time.Instant, Double>>()
        var time: java.time.Instant = first
        while (time.isBefore(sober)) {
            points.add(time to BacCalculator.bacAt(relevant, profile, time))
            time = time.plusSeconds(300)
        }
        points.add(sober to BacCalculator.bacAt(relevant, profile, sober))
        points
    }
    val end = data.endedAt ?: data.startedAt
    return SummaryUiState(
        context.getString(R.string.vm_summary_title, formatDate(data.startedAt.atZone(clock.zone).toLocalDate())),
        listOf(SessionMetricUiState(context.getString(R.string.vm_summary_start), formatTime(data.startedAt, clock.zone)), SessionMetricUiState(context.getString(R.string.vm_summary_end), formatTime(end, clock.zone)), SessionMetricUiState(context.getString(R.string.vm_summary_duration), durationText(context, Duration.between(data.startedAt, end).seconds)), SessionMetricUiState(context.getString(R.string.vm_summary_alcohol), formatGrams(logs.sumOf { it.pureAlcoholG }))),
        logs.groupBy { it.name to it.colorIndex }.map { (key, values) -> DrinkCategoryUiState(key.first, values.size, key.second) },
        series.map { (at, bac) -> BacPointUiState(formatTime(at, clock.zone), bac.toFloat()) },
        formatBac(if (profile == null) 0.0 else logs.maxOfOrNull { BacCalculator.bacAt(relevant, profile, it.drankAt) } ?: 0.0),
        if (last == null || profile == null) "—" else BacCalculator.soberAt(relevant, profile, last)?.let { formatTime(it, clock.zone) } ?: formatTime(last, clock.zone),
        logs.map { DrinkRowUiState(formatTime(it.drankAt, clock.zone), it.name, formatGrams(it.pureAlcoholG), it.colorIndex, it.id) },
        when (data.hcSyncStatus) { HcSyncStatus.SYNCED -> HealthSyncUiStatus.Sent; HcSyncStatus.FAILED -> HealthSyncUiStatus.Failed; HcSyncStatus.NOT_SYNCED -> HealthSyncUiStatus.NotLinked },
        logs.count { it.hcRecordId != null },
    )
}

@HiltViewModel
class SummaryViewModel @Inject constructor(handle: SavedStateHandle, data: ObservePartyData, settings: ObserveSettings, private val health: RequestHealthSync, private val close: CloseSummary, permissions: HealthPermissions, clock: Clock, @param:ApplicationContext context: Context) : ViewModel() {
    private val id: String = checkNotNull(handle["sessionId"])
    val uiState = combine(data.session(id), settings.profile, data.logs(), permissions.writeGranted) { session, profile, logs, granted -> session?.let { summaryState(it, profile, clock, context, logs).let { summary -> if (granted) summary else summary.copy(syncStatus = HealthSyncUiStatus.NotLinked) } } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun retry() = health(id)
    fun closeSummary(after: () -> Unit) { viewModelScope.launch { close(); after() } }
}
