package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.usecase.HealthPermissions
import tech.tamaki.alcoholmeter.ui.screens.summary.HealthSyncUiStatus
import tech.tamaki.alcoholmeter.domain.usecase.DeleteDrink
import tech.tamaki.alcoholmeter.domain.usecase.DeleteSession
import tech.tamaki.alcoholmeter.domain.usecase.ObservePartyData
import tech.tamaki.alcoholmeter.domain.usecase.ObservePresets
import tech.tamaki.alcoholmeter.domain.usecase.ObserveSettings
import tech.tamaki.alcoholmeter.domain.usecase.RequestHealthSync
import tech.tamaki.alcoholmeter.domain.usecase.UpdateDrink
import tech.tamaki.alcoholmeter.ui.format.formatDate
import tech.tamaki.alcoholmeter.ui.format.formatTime
import tech.tamaki.alcoholmeter.ui.screens.history.DrinkEditUiState
import tech.tamaki.alcoholmeter.ui.screens.history.HistoryDetailUiState
import tech.tamaki.alcoholmeter.ui.screens.history.timeWithinSession

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(handle: SavedStateHandle, data: ObservePartyData, settings: ObserveSettings, presets: ObservePresets, private val update: UpdateDrink, private val deleteDrink: DeleteDrink, private val deleteSession: DeleteSession, private val sync: RequestHealthSync, permissions: HealthPermissions, private val clock: Clock, @param:ApplicationContext context: Context) : ViewModel() {
    private val sessionId: String = checkNotNull(handle["sessionId"])
    private val editingId = MutableStateFlow<String?>(null)
    val uiState = combine(data.session(sessionId), settings.profile, presets(), editingId, data.logs()) { data, profile, list, editId, logs ->
        data?.let { session ->
            val start = session.session.startedAt
            val end = session.session.endedAt ?: start
            val log = session.logs.firstOrNull { it.id == editId }
            val edit = log?.let { drink -> DrinkEditUiState(drink.name, drink.presetId?.takeIf { id -> list.any { it.id == id } } ?: list.firstOrNull { it.name == drink.name && it.beverageType == drink.beverageType && it.volumeMl == drink.volumeMl && it.abvPercent == drink.abvPercent }?.id ?: 0, formatTime(drink.drankAt, clock.zone), formatDate(drink.drankAt.atZone(clock.zone).toLocalDate()), list, start.atZone(clock.zone).toLocalDateTime(), end.atZone(clock.zone).toLocalDateTime(), drink.drankAt.atZone(clock.zone).toLocalDateTime()) }
            HistoryDetailUiState(summaryState(session, profile, clock, context, logs), formatTime(start, clock.zone), formatTime(end, clock.zone), edit)
        }
    }.let { details -> combine(details, permissions.writeGranted) { detail, granted ->
        if (detail == null || granted) detail else detail.copy(summary = detail.summary.copy(syncStatus = HealthSyncUiStatus.NotLinked))
    } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    fun edit(id: String) { editingId.value = id }
    fun cancelEdit() { editingId.value = null }
    fun save(value: DrinkEditUiState) {
        val id = editingId.value ?: return
        if (!value.timeWithinSession) return
        val time = value.selectedDateTime.atZone(clock.zone).toInstant()
        viewModelScope.launch { update(id, value.selectedPresetId.takeIf { it != 0L }, time); editingId.value = null }
    }
    fun deleteDrink() { val id = editingId.value ?: return; viewModelScope.launch { deleteDrink(id); editingId.value = null } }
    fun deleteSession(after: () -> Unit) { viewModelScope.launch { deleteSession(sessionId); after() } }
    fun retry() = sync(sessionId)
}
