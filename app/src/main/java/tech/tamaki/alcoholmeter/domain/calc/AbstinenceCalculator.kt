package tech.tamaki.alcoholmeter.domain.calc

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import tech.tamaki.alcoholmeter.domain.model.DrinkLog

enum class DayStatus { DRINKING, ABSTINENT, TODAY, FUTURE, BEFORE_RECORDING }
data class MonthStats(
    val drinkingDays: Int,
    val abstinentDays: Int,
    val totalAlcoholG: Double,
    val averageAlcoholG: Double,
)
/** 論理日は記録時の現地時刻から 3 時間引いて求める。量は g。 */
object AbstinenceCalculator {
    fun logicalDate(at: Instant, offset: ZoneOffset): LocalDate = at.atOffset(offset).minusHours(3).toLocalDate()
    fun currentStreak(logs: List<DrinkLog>, recordStartDate: LocalDate, today: LocalDate): Long {
        val last = logs.map { logicalDate(it.drankAt, it.offset) }
            .filter { !it.isBefore(recordStartDate) && !it.isAfter(today) }
            .maxOrNull()
        return maxOf(0, ChronoUnit.DAYS.between(last?.plusDays(1) ?: recordStartDate, today))
    }
    fun longestStreak(logs: List<DrinkLog>, recordStartDate: LocalDate, today: LocalDate): Long {
        var start = recordStartDate
        var longest = 0L
        logs.map { logicalDate(it.drankAt, it.offset) }
            .filter { !it.isBefore(recordStartDate) && it.isBefore(today) }
            .distinct().sorted().forEach {
            longest = maxOf(longest, ChronoUnit.DAYS.between(start, it))
            start = it.plusDays(1)
        }
        return maxOf(longest, maxOf(0, ChronoUnit.DAYS.between(start, today)))
    }
    fun dayStatus(logs: List<DrinkLog>, recordStartDate: LocalDate, today: LocalDate, month: YearMonth): Map<LocalDate, DayStatus> {
        val drinking = logs.map { logicalDate(it.drankAt, it.offset) }.toSet()
        return (1..month.lengthOfMonth()).associate { day ->
            val date = month.atDay(day)
            date to when {
                date.isBefore(recordStartDate) -> DayStatus.BEFORE_RECORDING
                date.isAfter(today) -> DayStatus.FUTURE
                date in drinking -> DayStatus.DRINKING
                date == today -> DayStatus.TODAY
                else -> DayStatus.ABSTINENT
            }
        }
    }
    fun monthStats(logs: List<DrinkLog>, recordStartDate: LocalDate, today: LocalDate, month: YearMonth): MonthStats {
        val statuses = dayStatus(logs, recordStartDate, today, month)
        val relevant = logs.filter {
            val date = logicalDate(it.drankAt, it.offset)
            YearMonth.from(date) == month && !date.isBefore(recordStartDate) && !date.isAfter(today)
        }
        val total = relevant.sumOf { it.pureAlcoholG }
        val days = statuses.values.count {
            it == DayStatus.DRINKING || it == DayStatus.ABSTINENT || it == DayStatus.TODAY
        }
        return MonthStats(
            statuses.values.count { it == DayStatus.DRINKING },
            statuses.values.count { it == DayStatus.ABSTINENT },
            total,
            if (days == 0) 0.0 else total / days,
        )
    }
}
