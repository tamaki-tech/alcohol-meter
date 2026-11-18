package tech.tamaki.alcoholmeter.domain.calc

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tech.tamaki.alcoholmeter.domain.model.DrinkLog
import tech.tamaki.alcoholmeter.domain.model.Sex
import tech.tamaki.alcoholmeter.domain.model.UserProfile

class CalculatorsTest {
    private val t = Instant.parse("2026-01-01T12:00:00Z")
    private val male = UserProfile(60.0, Sex.MALE)
    private val rise = 14.0 / (60 * 0.68 * 10)
    private fun log(at: Instant, offset: ZoneOffset = ZoneOffset.UTC) =
        DrinkLog("1", "s", at, offset, "beer", 0, 350.0, 5.0, 14.0, 0, null)
    private fun day(s: String) = LocalDate.parse(s)
    private fun drink(s: String) = log(Instant.parse("${s}T12:00:00Z"))

    @Test fun pureAlcoholUsesVolumeAndPercent() {
        assertEquals(14.0, PureAlcohol.grams(350.0, 5.0), 1e-9)
    }
    @Test fun bacWithNoDrinksIsZero() {
        assertEquals(0.0, BacCalculator.bacAt(emptyList(), male, t))
    }
    @Test fun bacWithOneDrinkUsesWidmarkFormula() {
        assertEquals(rise, BacCalculator.bacAt(listOf(log(t)), male, t), 1e-9)
    }
    @Test fun bacAccumulatesDrinksAfterElimination() {
        val next = t.plusSeconds(3600)
        assertEquals(2 * rise - 0.015, BacCalculator.bacAt(listOf(log(next), log(t)), male, next), 1e-9)
    }
    @Test fun bacReturnsToZeroAfterLongInterval() {
        assertEquals(0.0, BacCalculator.bacAt(listOf(log(t)), male, t.plusSeconds(36000)))
    }
    @Test fun drinkAfterBacReturnsToZeroStartsFresh() {
        val next = t.plusSeconds(36000)
        assertEquals(rise, BacCalculator.bacAt(listOf(log(t), log(next)), male, next), 1e-9)
    }
    @Test fun femaleCoefficientIs055() {
        assertEquals(14.0 / (60 * 0.55 * 10), BacCalculator.bacAt(listOf(log(t)), UserProfile(60.0, Sex.FEMALE), t), 1e-9)
    }
    @Test fun soberAtCalculatesRemainingTimeAndReturnsNullAtZero() {
        val sober = BacCalculator.soberAt(listOf(log(t)), male, t)!!
        assertEquals(rise / 0.015 * 3600, Duration.between(t, sober).toNanos() / 1e9, 1e-6)
        assertNull(BacCalculator.soberAt(listOf(log(t)), male, t.plusSeconds(36000)))
    }
    @Test fun seriesUsesFiveMinuteStepsAndIncludesSoberEndpoint() {
        val points = BacCalculator.series(listOf(log(t)), male)
        assertEquals(t, points.first().first)
        points.dropLast(1).zipWithNext().forEach { (a, b) -> assertEquals(Duration.ofMinutes(5), Duration.between(a.first, b.first)) }
        assertEquals(BacCalculator.soberAt(listOf(log(t)), male, t), points.last().first)
        assertEquals(0.0, points.last().second, 1e-9)
    }
    @Test fun logicalDateChangesAtThree() {
        val offset = ZoneOffset.ofHours(9)
        assertEquals(day("2026-01-01"), AbstinenceCalculator.logicalDate(Instant.parse("2026-01-01T17:59:00Z"), offset))
        assertEquals(day("2026-01-02"), AbstinenceCalculator.logicalDate(Instant.parse("2026-01-01T18:00:00Z"), offset))
    }
    @Test fun streakCrossesMonthBoundary() {
        assertEquals(2L, AbstinenceCalculator.currentStreak(listOf(drink("2026-01-31")), day("2026-01-30"), day("2026-02-03")))
    }
    @Test fun logOffsetsDetermineTheirOwnLogicalDates() {
        val instant = Instant.parse("2026-01-01T18:00:00Z")
        val logs = listOf(log(instant, ZoneOffset.UTC), log(instant, ZoneOffset.ofHours(9)))
        val statuses = AbstinenceCalculator.dayStatus(logs, day("2026-01-01"), day("2026-01-03"), YearMonth.of(2026, 1))
        assertEquals(DayStatus.DRINKING, statuses[day("2026-01-01")])
        assertEquals(DayStatus.DRINKING, statuses[day("2026-01-02")])
    }
    @Test fun logsBeforeRecordingAreIgnoredForStreak() {
        assertEquals(3L, AbstinenceCalculator.currentStreak(listOf(drink("2026-01-01")), day("2026-01-03"), day("2026-01-06")))
    }
    @Test fun noDrinksCountsFromStart() {
        assertEquals(3L, AbstinenceCalculator.currentStreak(emptyList(), day("2026-01-03"), day("2026-01-06")))
    }
    @Test fun drinkingTodayResetsCurrentStreak() {
        assertEquals(0L, AbstinenceCalculator.currentStreak(listOf(drink("2026-01-06")), day("2026-01-01"), day("2026-01-06")))
    }
    @Test fun longestIncludesStartToFirstDrink() {
        val logs = listOf(drink("2026-01-05"), drink("2026-01-07"))
        assertEquals(4L, AbstinenceCalculator.longestStreak(logs, day("2026-01-01"), day("2026-01-09")))
    }
    @Test fun monthStatsCountsDaysAndAlcohol() {
        val logs = listOf(drink("2026-01-02"), drink("2026-01-02"), drink("2026-01-04"))
        val stats = AbstinenceCalculator.monthStats(logs, day("2026-01-01"), day("2026-01-04"), YearMonth.of(2026, 1))
        assertEquals(MonthStats(2, 2, 42.0, 10.5), stats)
    }
}
