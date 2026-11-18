package tech.tamaki.alcoholmeter.data.health

import java.time.Instant
import java.time.ZoneOffset
import tech.tamaki.alcoholmeter.domain.model.DrinkLog

/** Android の API 型に依存しない、1 杯の Health Connect 書き込み内容。 */
data class AlcoholRecordData(
    val clientRecordId: String,
    val time: Instant,
    val offset: ZoneOffset,
    val beverageType: Int,
    val servingVolumeLiters: Double,
    val alcoholByVolumePercent: Double,
    val notes: String,
)

fun DrinkLog.toAlcoholRecordData() = AlcoholRecordData(
    clientRecordId = id,
    time = drankAt,
    offset = offset,
    beverageType = beverageType,
    servingVolumeLiters = volumeMl / 1000.0,
    alcoholByVolumePercent = abvPercent,
    notes = name,
)
