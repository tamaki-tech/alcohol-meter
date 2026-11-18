package tech.tamaki.alcoholmeter.ui.viewmodel

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import tech.tamaki.alcoholmeter.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.model.Sex
import tech.tamaki.alcoholmeter.domain.usecase.HealthPermissions
import tech.tamaki.alcoholmeter.domain.usecase.ObservePresets
import tech.tamaki.alcoholmeter.domain.usecase.ObserveSettings
import tech.tamaki.alcoholmeter.domain.usecase.ReconcileBackgroundState
import tech.tamaki.alcoholmeter.domain.usecase.ReorderPresets
import tech.tamaki.alcoholmeter.domain.usecase.SaveOnboardingProfile
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import tech.tamaki.alcoholmeter.ui.screens.settings.SettingsUiState

@HiltViewModel
class SettingsViewModel @Inject constructor(presets: ObservePresets, settings: ObserveSettings, private val reorder: ReorderPresets, private val health: HealthPermissions, private val reconcile: ReconcileBackgroundState, private val saveProfile: SaveOnboardingProfile, @param:ApplicationContext private val context: Context) : ViewModel() {
    val profile = settings.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val permissions get() = health.required
    fun permissionContract() = health.contract()
    fun updateProfile(value: UserProfile) {
        if (value.weightKg.isFinite() && value.weightKg > 0 && value.eliminationRatePerHour.isFinite() && value.eliminationRatePerHour > 0) viewModelScope.launch { saveProfile(value) }
    }
    private val granted = MutableStateFlow(false)
    init { refreshHealth() }
    fun refreshHealth() { viewModelScope.launch { health.refresh(); granted.value = health.granted(); if (health.writeGranted.value) reconcile() } }
    val uiState = combine(presets(), settings.profile, granted) { list, profile, permitted ->
        SettingsUiState(list, list.count { it.showInNotification }, profile?.let { context.getString(R.string.vm_weight_value, it.weightKg.toString()) } ?: "—", profile?.let { if (it.sex == Sex.MALE) context.getString(R.string.onboarding_male) else context.getString(R.string.onboarding_female) } ?: "—", profile?.let { context.getString(R.string.vm_rate_value, it.eliminationRatePerHour.toString()) } ?: "—", context.getString(if (permitted) R.string.onboarding_hc_granted else R.string.sync_not_linked))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(emptyList(), 0, "—", "—", "—", "—"))
    fun move(fromId: Long, toId: Long) {
        val ids = uiState.value.presets.map { it.id }.toMutableList()
        val from = ids.indexOf(fromId)
        val to = ids.indexOf(toId)
        if (from < 0 || to < 0) return
        ids.add(to, ids.removeAt(from))
        viewModelScope.launch { reorder(ids) }
    }
}
