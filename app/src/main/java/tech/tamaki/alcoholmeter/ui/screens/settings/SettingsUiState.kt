package tech.tamaki.alcoholmeter.ui.screens.settings

import tech.tamaki.alcoholmeter.domain.model.DrinkPreset

data class SettingsUiState(
    val presets: List<DrinkPreset>,
    val notificationCount: Int,
    val weight: String,
    val sex: String,
    val eliminationRate: String,
    val healthConnect: String,
)

data class PresetEditorUiState(
    val preset: DrinkPreset,
    val pureAlcohol: String,
    val notificationEnabled: Boolean,
    val notificationLimitReached: Boolean,
)

data class PresetEditorDraft(
    val name: String,
    val volumeMl: String,
    val abvPercent: String,
    val beverageType: Int,
    val colorIndex: Int,
    val notificationEnabled: Boolean,
)
