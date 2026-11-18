package tech.tamaki.alcoholmeter.domain.usecase

import javax.inject.Inject
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.domain.repository.PresetRepository

class SavePreset @Inject constructor(private val repository: PresetRepository) {
    suspend operator fun invoke(preset: DrinkPreset) = repository.upsert(preset)
}
class ReorderPresets @Inject constructor(private val repository: PresetRepository) {
    suspend operator fun invoke(ids: List<Long>) = repository.reorder(ids)
}
