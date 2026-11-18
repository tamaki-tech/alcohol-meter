package tech.tamaki.alcoholmeter.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatTest {
    @Test fun formatsBacToThreeDecimalPlaces() = assertEquals("0.045%", formatBac(0.0453))
    @Test fun formatsAlcoholToOneDecimalPlace() = assertEquals("14.0 g", formatGrams(14.0))
    @Test fun formatsTimeInRequestedZone() = assertEquals("21:05", formatTime(Instant.parse("2026-11-18T12:05:00Z"), ZoneOffset.ofHours(9)))
    @Test fun formatsJapaneseDate() = assertEquals("11/18（水）", formatDate(LocalDate.of(2026, 11, 18)))
    @Test fun formatsElapsedHoursMinutesSeconds() = assertEquals("1:02:03", formatDuration(3723))
    @Test fun formatsShortDurationInMinutes() = assertEquals(DurationParts(0, 42), durationParts(42 * 60 + 59))
    @Test fun formatsLongDurationInHoursAndMinutes() = assertEquals(DurationParts(2, 42), durationParts(2 * 3600 + 42 * 60))
    @Test fun formatsZeroDuration() = assertEquals(DurationParts(0, 0), durationParts(0))
}
