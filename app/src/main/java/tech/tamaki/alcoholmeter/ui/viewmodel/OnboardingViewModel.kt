package tech.tamaki.alcoholmeter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.tamaki.alcoholmeter.domain.model.HcAvailability
import tech.tamaki.alcoholmeter.domain.model.Sex
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import tech.tamaki.alcoholmeter.domain.usecase.CheckHealthAvailability
import tech.tamaki.alcoholmeter.domain.usecase.CompleteOnboarding
import tech.tamaki.alcoholmeter.domain.usecase.HealthPermissions
import tech.tamaki.alcoholmeter.domain.usecase.ObserveSettings
import tech.tamaki.alcoholmeter.domain.usecase.ReconcileBackgroundState
import tech.tamaki.alcoholmeter.domain.usecase.SaveOnboardingProfile
import tech.tamaki.alcoholmeter.ui.format.formatDate
import tech.tamaki.alcoholmeter.ui.screens.onboarding.OnboardingSex
import tech.tamaki.alcoholmeter.ui.screens.onboarding.OnboardingUiState

sealed interface LaunchState { data object Loading : LaunchState; data object Unsupported : LaunchState; data object Onboarding : LaunchState; data object Ready : LaunchState }

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val availability: CheckHealthAvailability,
    private val health: HealthPermissions,
    private val settings: ObserveSettings,
    private val saveProfile: SaveOnboardingProfile,
    private val complete: CompleteOnboarding,
    private val reconcile: ReconcileBackgroundState,
    private val clock: Clock,
) : ViewModel() {
    private val mutableLaunch = MutableStateFlow<LaunchState>(LaunchState.Loading)
    val launchState = mutableLaunch.asStateFlow()
    private val mutableState = MutableStateFlow(OnboardingUiState(1, "", null, null, false))
    val uiState = mutableState.asStateFlow()
    val permissions get() = health.required
    fun permissionContract() = health.contract()
    init { viewModelScope.launch {
        if (availability() != HcAvailability.AVAILABLE) mutableLaunch.value = LaunchState.Unsupported
        else if (settings.onboardingCompleted.first()) { mutableLaunch.value = LaunchState.Ready; reconcile() }
        else {
            settings.profile.first()?.let { saved ->
                mutableState.update { it.copy(weightKg = saved.weightKg.toString(), sex = if (saved.sex == Sex.MALE) OnboardingSex.Male else OnboardingSex.Female) }
            }
            mutableLaunch.value = LaunchState.Onboarding
            refreshHealth()
        }
    } }
    fun setWeight(value: String) { mutableState.update { it.copy(weightKg = value) } }
    fun setSex(value: OnboardingSex) { mutableState.update { it.copy(sex = value) } }
    fun next() { viewModelScope.launch {
        val state = mutableState.value
        val weight = state.weightKg.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return@launch
        val sex = state.sex ?: return@launch
        saveProfile(UserProfile(weight, if (sex == OnboardingSex.Male) Sex.MALE else Sex.FEMALE))
        mutableState.update { it.copy(step = 2) }
    } }
    fun healthResult() { viewModelScope.launch { refreshHealth(); mutableState.update { it.copy(step = 3) } } }
    fun skip() { if (mutableState.value.step == 2) mutableState.update { it.copy(step = 3) } else finish() }
    fun finish() { viewModelScope.launch { complete(); mutableLaunch.value = LaunchState.Ready; reconcile() } }
    private suspend fun refreshHealth() {
        health.refresh()
        val granted = health.granted()
        val latest = try { health.latestWeight() } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled } catch (_: Exception) { null }
        mutableState.update { state -> state.copy(healthConnectGranted = granted, weightKg = if (state.weightKg.isBlank() && latest != null) latest.first.toString() else state.weightKg, healthConnectWeightDate = latest?.second?.atZone(clock.zone)?.toLocalDate()?.let(::formatDate)) }
    }
}
