package tech.tamaki.alcoholmeter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import tech.tamaki.alcoholmeter.domain.calc.AbstinenceCalculator
import tech.tamaki.alcoholmeter.domain.usecase.ObservePartyData
import tech.tamaki.alcoholmeter.domain.usecase.ObserveSettings
import tech.tamaki.alcoholmeter.ui.format.formatGrams
import tech.tamaki.alcoholmeter.ui.screens.calendar.CalendarUiState

@HiltViewModel
class CalendarViewModel @Inject constructor(private val clock: Clock, data: ObservePartyData, settings: ObserveSettings) : ViewModel() {
    private fun today() = clock.instant().let { AbstinenceCalculator.logicalDate(it, clock.zone.rules.getOffset(it)) }
    private val month = MutableStateFlow(YearMonth.from(today()))
    val uiState = combine(data.logs(), settings.recordStartDate, month) { logs, first, selected ->
        val today = today()
        val start = first ?: today
        val stats = AbstinenceCalculator.monthStats(logs, start, today, selected)
        CalendarUiState(selected, today, AbstinenceCalculator.dayStatus(logs, start, today, selected), AbstinenceCalculator.currentStreak(logs, start, today).toInt(), AbstinenceCalculator.longestStreak(logs, start, today).toInt(), stats.abstinentDays, stats.drinkingDays, formatGrams(stats.averageAlcoholG))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(month.value, today(), emptyMap(), 0, 0, 0, 0, formatGrams(0.0)))
    private val daySessions = data.logs().map { logs -> logs.groupBy { AbstinenceCalculator.logicalDate(it.drankAt, it.offset) }.mapValues { (_, items) -> items.maxByOrNull { it.drankAt }?.sessionId } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    fun sessionForDay(date: java.time.LocalDate): String? = daySessions.value[date]
    fun selectMonth(value: YearMonth) { if (value <= YearMonth.from(today())) month.value = value }
}
