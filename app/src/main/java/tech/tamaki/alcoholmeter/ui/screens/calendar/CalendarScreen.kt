package tech.tamaki.alcoholmeter.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.domain.calc.DayStatus
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

private data class CalendarCell(val date: LocalDate?, val status: DayStatus?, val isToday: Boolean)

@Composable
fun CalendarScreen(state: CalendarUiState, onMonthChange: (YearMonth) -> Unit, onDayClick: (LocalDate) -> Unit) {
    val todayMonth = YearMonth.from(state.today)
    val offset = state.month.atDay(1).dayOfWeek.value % 7
    val cells = (0 until offset).map { CalendarCell(null, null, false) } + (1..state.month.lengthOfMonth()).map { day ->
        val date = state.month.atDay(day)
        CalendarCell(date, state.dayStatuses[date] ?: DayStatus.BEFORE_RECORDING, date == state.today)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.calendar_month_title, state.month.year, state.month.monthValue), Modifier.weight(1f), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            AppButton("", { onMonthChange(state.month.minusMonths(1)) }, Modifier.size(48.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_chevron_left, iconContentDescription = stringResource(R.string.calendar_previous_month))
            val canAdvance = state.month < todayMonth
            AppButton("", { if (canAdvance) onMonthChange(state.month.plusMonths(1)) }, Modifier.size(48.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_chevron_right, iconContentDescription = stringResource(R.string.calendar_next_month), contentDescription = if (canAdvance) null else stringResource(R.string.calendar_next_month_disabled), enabled = canAdvance)
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
            val weekdays = listOf(R.string.weekday_sunday, R.string.weekday_monday, R.string.weekday_tuesday, R.string.weekday_wednesday, R.string.weekday_thursday, R.string.weekday_friday, R.string.weekday_saturday)
            weekdays.forEach { label -> Text(stringResource(label), Modifier.weight(1f), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) }
        }
        LazyVerticalGrid(GridCells.Fixed(7), Modifier.fillMaxWidth().height(276.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(cells) { cell -> CalendarDayCell(cell, onDayClick) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendItem(stringResource(R.string.calendar_rest_day), rest = true)
            LegendItem(stringResource(R.string.calendar_drinking_day), drinking = true)
            Text(stringResource(R.string.calendar_cutoff), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
            Row(Modifier.fillMaxWidth().height(70.dp)) {
                CalendarMetric(stringResource(R.string.calendar_current_streak), stringResource(R.string.calendar_days_value, state.currentStreak), Modifier.weight(1f), true)
                CalendarMetric(stringResource(R.string.calendar_longest_streak), stringResource(R.string.calendar_days_value, state.longestStreak), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().height(70.dp)) {
                CalendarMetric(stringResource(R.string.calendar_month_rest_days), stringResource(R.string.calendar_days_value, state.monthRestDays), Modifier.weight(1f))
                CalendarMetric(stringResource(R.string.calendar_drinking_days), stringResource(R.string.calendar_days_value, state.drinkingDays), Modifier.weight(1f))
            }
            CalendarMetric(stringResource(R.string.calendar_average_alcohol), state.averageAlcohol, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CalendarDayCell(cell: CalendarCell, onDayClick: (LocalDate) -> Unit) {
    val status = cell.status
    val today = cell.isToday || status == DayStatus.TODAY
    val future = status == DayStatus.FUTURE
    val shape = RoundedCornerShape(8.dp)
    Column(
        Modifier.fillMaxWidth().height(42.dp)
            .background(if (status == DayStatus.ABSTINENT) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f) else Color.Transparent, shape)
            .border(if (today) 1.dp else 0.dp, if (today) MaterialTheme.colorScheme.primary else Color.Transparent, shape)
            .then(if (cell.date != null && !future) Modifier.clickable { onDayClick(cell.date) } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(cell.date?.dayOfMonth?.toString().orEmpty(), fontSize = 14.sp, color = if (future || status == DayStatus.BEFORE_RECORDING) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface)
        when (status) {
            DayStatus.DRINKING -> DrinkingDayMark()
            DayStatus.ABSTINENT -> RestDayCheck()
            else -> Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun DrinkingDayMark() {
    val color = MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.Canvas(Modifier.size(5.dp)) { drawCircle(color) }
}

// 休肝日のチェックマーク。行の高さを変えないよう、数字の下に重ねて描く（レイアウト上は 5dp）
@Composable
private fun RestDayCheck() {
    val color = MaterialTheme.colorScheme.tertiary
    Box(Modifier.height(5.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.requiredSize(10.dp)) {
            val w = size.width
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.15f, w * 0.52f)
                lineTo(w * 0.40f, w * 0.77f)
                lineTo(w * 0.87f, w * 0.25f)
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.75.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

@Composable
private fun LegendItem(label: String, rest: Boolean = false, drinking: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (rest) Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { RestDayCheck() }
        if (drinking) DrinkingDayMark()
        Text(label, Modifier.padding(start = 5.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun CalendarMetric(label: String, value: String, modifier: Modifier, highlighted: Boolean = false) {
    Column(modifier.padding(vertical = 8.dp)) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Preview(name = "Calendar Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CalendarLightPreview() { AlcoholMeterTheme(false) { Surface { CalendarScreen(previewCalendar(), {}, {}) } } }

@Preview(name = "Calendar Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun CalendarDarkPreview() { AlcoholMeterTheme(true) { Surface { CalendarScreen(previewCalendar(), {}, {}) } } }

@Preview(name = "Calendar 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun CalendarLargeTextPreview() { AlcoholMeterTheme(true) { Surface { CalendarScreen(previewCalendar(), {}, {}) } } }

private fun previewCalendar(): CalendarUiState {
    val month = YearMonth.of(2026, 11)
    val today = LocalDate.of(2026, 11, 18)
    val statuses = (1..month.lengthOfMonth()).associate { day ->
        val date = month.atDay(day)
        date to when {
            date.isAfter(today) -> DayStatus.FUTURE
            date == today -> DayStatus.TODAY
            date.dayOfMonth == 14 -> DayStatus.DRINKING
            else -> DayStatus.ABSTINENT
        }
    }
    return CalendarUiState(month, today, statuses, 12, 28, 9, 5, "16.4 g")
}
