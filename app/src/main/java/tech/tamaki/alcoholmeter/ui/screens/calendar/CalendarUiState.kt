package tech.tamaki.alcoholmeter.ui.screens.calendar

import java.time.LocalDate
import java.time.YearMonth
import tech.tamaki.alcoholmeter.domain.calc.DayStatus

data class CalendarUiState(
    val month: YearMonth,
    val today: LocalDate,
    val dayStatuses: Map<LocalDate, DayStatus>,
    val currentStreak: Int,
    val longestStreak: Int,
    val monthRestDays: Int,
    val drinkingDays: Int,
    val averageAlcohol: String,
)
