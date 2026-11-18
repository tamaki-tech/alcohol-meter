package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.tamaki.alcoholmeter.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import tech.tamaki.alcoholmeter.domain.model.HcSyncStatus
import tech.tamaki.alcoholmeter.domain.usecase.ObservePartyData
import tech.tamaki.alcoholmeter.domain.usecase.RequestHealthSync
import tech.tamaki.alcoholmeter.domain.usecase.HealthPermissions
import tech.tamaki.alcoholmeter.ui.format.formatGrams
import tech.tamaki.alcoholmeter.ui.format.formatTime
import tech.tamaki.alcoholmeter.ui.screens.history.HistoryListUiState
import tech.tamaki.alcoholmeter.ui.screens.history.HistorySessionUiState
import tech.tamaki.alcoholmeter.ui.screens.history.HistorySyncStatus

@HiltViewModel
class HistoryListViewModel @Inject constructor(data: ObservePartyData, clock: Clock, private val sync: RequestHealthSync, permissions: HealthPermissions, @param:ApplicationContext context: Context) : ViewModel() {
    val uiState = combine(data.history(), permissions.writeGranted) { sessions, granted -> HistoryListUiState(sessions.map { item ->
        val date = item.session.startedAt.atZone(clock.zone).toLocalDate()
        HistorySessionUiState(item.session.id, context.getString(R.string.vm_month_heading, date.year, date.monthValue), date.format(DateTimeFormatter.ofPattern("MM/dd", Locale.JAPAN)), date.format(DateTimeFormatter.ofPattern("E", Locale.JAPAN)), "${formatTime(item.session.startedAt, clock.zone)}–${item.session.endedAt?.let { formatTime(it, clock.zone) } ?: "—"}", context.getString(R.string.vm_history_details, item.logs.size, formatGrams(item.logs.sumOf { it.pureAlcoholG })), if (!granted) HistorySyncStatus.NotLinked else when (item.session.hcSyncStatus) { HcSyncStatus.SYNCED -> HistorySyncStatus.Sent; HcSyncStatus.FAILED -> HistorySyncStatus.Failed; HcSyncStatus.NOT_SYNCED -> HistorySyncStatus.NotLinked })
    }) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryListUiState(emptyList()))
    fun retry(id: String) = sync(id)
}
