package tech.tamaki.alcoholmeter.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatBac(value: Double): String = String.format(Locale.ROOT, "%.3f%%", value)
fun formatGrams(value: Double): String = String.format(Locale.ROOT, "%.1f g", value)
fun formatTime(value: Instant, zone: ZoneId): String = DateTimeFormatter.ofPattern("HH:mm", Locale.JAPAN).withZone(zone).format(value)
fun formatDate(value: LocalDate): String = DateTimeFormatter.ofPattern("M/d（E）", Locale.JAPAN).format(value)
fun formatDuration(seconds: Long): String {
    val value = seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d:%02d", value / 3600, value / 60 % 60, value % 60)
}

data class DurationParts(val hours: Long, val minutes: Long)

/** 所要秒数を分単位に切り捨て、時間と分に分解する。 */
fun durationParts(seconds: Long): DurationParts {
    val minutes = seconds.coerceAtLeast(0) / 60
    return DurationParts(minutes / 60, minutes % 60)
}
