package tech.tamaki.alcoholmeter.data.health

import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.tamaki.alcoholmeter.domain.model.DrinkLog

class AlcoholRecordMappingTest {
    @Test fun mapsOneDrinkToHealthConnectFields() {
        val time = Instant.parse("2026-01-01T12:00:00Z")
        val offset = ZoneOffset.ofHours(9)
        val log = DrinkLog("drink-id", "session", time, offset, "ビール", 1, 350.0, 5.0, 14.0, 0, null)
        assertEquals(AlcoholRecordData("drink-id", time, offset, 1, 0.35, 5.0, "ビール"), log.toAlcoholRecordData())
    }
}
