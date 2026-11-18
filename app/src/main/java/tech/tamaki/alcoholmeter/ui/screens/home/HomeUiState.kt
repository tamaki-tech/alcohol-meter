package tech.tamaki.alcoholmeter.ui.screens.home

import tech.tamaki.alcoholmeter.domain.model.DrinkPreset

data class HomeBeforeUiState(
    val date: String,
    val streakDays: Int,
    val longestStreakDays: Int,
    val monthRestDays: Int,
    val recentParty: String?,
    val previousBac: String? = null,
    val soberEta: String? = null,
)

data class HomeRecentDrinkUiState(
    val name: String,
    val time: String,
    val colorIndex: Int,
)

data class HomeInPartyUiState(
    val startedAt: String,
    val elapsed: String,
    val cups: Int,
    val bac: String,
    val pureAlcohol: String,
    val recentDrinks: List<HomeRecentDrinkUiState>,
    val presets: List<DrinkPreset>,
    val drinkCounts: Map<Long, Int>,
    val lastPresetId: Long?,
    val undoMessage: String? = null,
)
