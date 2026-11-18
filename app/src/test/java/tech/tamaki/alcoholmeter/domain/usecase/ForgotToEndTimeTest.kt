package tech.tamaki.alcoholmeter.domain.usecase

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ForgotToEndTimeTest {
    @Test fun reminderStartsExactlySixHoursAfterLastDrink() {
        val drink = Instant.parse("2026-01-01T00:00:00Z")
        assertFalse(shouldRemindToEnd(drink, drink.plusSeconds(6 * 3600 - 1)))
        assertTrue(shouldRemindToEnd(drink, drink.plusSeconds(6 * 3600)))
    }
    @Test fun noDrinkDoesNotScheduleReminder() {
        assertFalse(shouldRemindToEnd(null, Instant.parse("2026-01-01T00:00:00Z")))
    }
}
