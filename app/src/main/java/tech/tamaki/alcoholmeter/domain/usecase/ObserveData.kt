package tech.tamaki.alcoholmeter.domain.usecase

import javax.inject.Inject
import tech.tamaki.alcoholmeter.domain.repository.PartyRepository
import tech.tamaki.alcoholmeter.domain.repository.PresetRepository
import tech.tamaki.alcoholmeter.domain.repository.SettingsRepository

class ObservePartyData @Inject constructor(private val party: PartyRepository) {
    fun logs() = party.observeAllLogs()
    fun history() = party.observeHistory()
    fun session(id: String) = party.observeSession(id)
}
class ObservePresets @Inject constructor(private val presets: PresetRepository) {
    operator fun invoke() = presets.observePresets()
}
class ObserveSettings @Inject constructor(private val settings: SettingsRepository) {
    val profile get() = settings.profile
    val recordStartDate get() = settings.recordStartDate
    val onboardingCompleted get() = settings.onboardingCompleted
}
