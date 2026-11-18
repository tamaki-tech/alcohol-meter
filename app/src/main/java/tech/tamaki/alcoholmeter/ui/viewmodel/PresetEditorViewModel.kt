package tech.tamaki.alcoholmeter.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.calc.PureAlcohol
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.domain.usecase.ObservePresets
import tech.tamaki.alcoholmeter.domain.usecase.SavePreset
import tech.tamaki.alcoholmeter.ui.format.formatGrams
import tech.tamaki.alcoholmeter.ui.screens.settings.PresetEditorDraft
import tech.tamaki.alcoholmeter.ui.screens.settings.PresetEditorUiState

@HiltViewModel
class PresetEditorViewModel @Inject constructor(handle: SavedStateHandle, presets: ObservePresets, private val save: SavePreset) : ViewModel() {
    private val id: Long = handle.get<Long>("presetId") ?: 0L
    private val edited = MutableStateFlow<PresetEditorDraft?>(null)
    private val default = DrinkPreset(0, "", "", 0, 0.0, 0.0, "beer", 0, 0, false)
    val uiState = combine(presets(), edited) { list, draft ->
        val original = list.firstOrNull { it.id == id } ?: default.copy(sortOrder = list.size)
        val selected = draft ?: PresetEditorDraft(original.name, original.volumeMl.toString(), original.abvPercent.toString(), original.beverageType, original.colorIndex, original.showInNotification)
        val count = list.count { it.showInNotification && it.id != original.id }
        val volume = selected.volumeMl.toDoubleOrNull() ?: 0.0
        val abv = selected.abvPercent.toDoubleOrNull() ?: 0.0
        PresetEditorUiState(original, formatGrams(PureAlcohol.grams(volume, abv)), selected.notificationEnabled, count >= 3 && !selected.notificationEnabled) to selected
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PresetEditorUiState(default, formatGrams(0.0), false, false) to PresetEditorDraft("", "", "", 0, 0, false))
    fun edit(draft: PresetEditorDraft) {
        if (draft.notificationEnabled && uiState.value.first.notificationLimitReached) return
        edited.value = draft
    }
    fun save(onSaved: () -> Unit) {
        val (state, draft) = uiState.value
        val volume = draft.volumeMl.toDoubleOrNull() ?: return
        val abv = draft.abvPercent.toDoubleOrNull() ?: return
        if (draft.name.isBlank() || !volume.isFinite() || volume <= 0 || !abv.isFinite() || abv !in 0.0..100.0) return
        viewModelScope.launch {
            save(state.preset.copy(name = draft.name, shortName = if (draft.name == state.preset.name) state.preset.shortName else draft.name, volumeMl = volume, abvPercent = abv, beverageType = draft.beverageType, colorIndex = draft.colorIndex, showInNotification = draft.notificationEnabled))
            onSaved()
        }
    }
}
