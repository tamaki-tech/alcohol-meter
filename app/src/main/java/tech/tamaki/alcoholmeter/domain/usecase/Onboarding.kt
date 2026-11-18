package tech.tamaki.alcoholmeter.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.tamaki.alcoholmeter.domain.model.UserProfile
import tech.tamaki.alcoholmeter.domain.repository.HealthConnectRepository
import tech.tamaki.alcoholmeter.domain.repository.SettingsRepository

class CheckHealthAvailability @Inject constructor(private val health: HealthConnectRepository) {
    suspend operator fun invoke() = health.availability()
}
@Singleton
class HealthPermissions @Inject constructor(private val health: HealthConnectRepository) {
    private val mutableWriteGranted = MutableStateFlow(false)
    val writeGranted = mutableWriteGranted.asStateFlow()
    suspend fun refresh() { mutableWriteGranted.value = health.hasWritePermission() }
    val required get() = health.requiredPermissions
    fun contract() = health.permissionContract()
    suspend fun granted() = health.hasPermissions()
    suspend fun latestWeight() = health.latestWeight()
}
class SaveOnboardingProfile @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke(profile: UserProfile) = settings.saveProfile(profile)
}
class CompleteOnboarding @Inject constructor(private val settings: SettingsRepository) {
    suspend operator fun invoke() = settings.completeOnboarding()
}
