package tech.tamaki.alcoholmeter.ui.screens.history

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.components.DrinkColorDot
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@Composable
fun HistoryListScreen(
    state: HistoryListUiState,
    onOpenSession: (HistorySessionUiState) -> Unit,
    onRetrySync: (HistorySessionUiState) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 16.dp)) {
        Text(stringResource(R.string.nav_history), Modifier.padding(horizontal = 16.dp), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        state.sessions.groupBy { it.monthHeading }.forEach { (month, sessions) ->
            Text(month, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, letterSpacing = 0.7.sp)
            sessions.forEach { session ->
                HistorySessionRow(session, { onOpenSession(session) }, { onRetrySync(session) })
            }
        }
    }
}

@Composable
private fun HistorySessionRow(session: HistorySessionUiState, onOpen: () -> Unit, onRetry: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(68.dp).borderBottom().clickable(onClick = onOpen).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 12.dp)) {
            Text(session.date, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(session.weekday, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Column(Modifier.weight(1f)) {
            Text(session.timeRange, fontSize = 14.sp, maxLines = 1)
            Text(session.details, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        }
        val (label, color) = when (session.status) {
            HistorySyncStatus.Sent -> stringResource(R.string.history_sent) to MaterialTheme.colorScheme.secondary
            HistorySyncStatus.Failed -> stringResource(R.string.history_failed) to MaterialTheme.colorScheme.primary
            HistorySyncStatus.NotLinked -> stringResource(R.string.history_not_linked) to MaterialTheme.colorScheme.secondary
        }
        Text(
            label,
            Modifier.border(1.dp, color, RoundedCornerShape(999.dp)).clickable(onClick = onRetry).padding(horizontal = 8.dp, vertical = 5.dp),
            fontSize = 11.sp,
            color = color,
            maxLines = 1,
        )
    }
}

private fun Modifier.borderBottom(): Modifier = this.then(Modifier)

private val previewHistory = HistoryListUiState(
    listOf(
        HistorySessionUiState("1", "2026年 11月", "11/14", "金", "19:02–23:40", "7 杯 ・ 114.8 g", HistorySyncStatus.Sent),
        HistorySessionUiState("2", "2026年 11月", "11/10", "月", "20:12–22:20", "4 杯 ・ 72.0 g", HistorySyncStatus.Failed),
        HistorySessionUiState("3", "2026年 10月", "10/28", "水", "19:00–22:30", "5 杯 ・ 90.0 g", HistorySyncStatus.NotLinked),
    ),
)

@Preview(name = "History List Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HistoryListLightPreview() {
    AlcoholMeterTheme(false) { Surface { HistoryListScreen(previewHistory, {}, {}) } }
}

@Preview(name = "History List Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HistoryListDarkPreview() {
    AlcoholMeterTheme(true) { Surface { HistoryListScreen(previewHistory, {}, {}) } }
}

@Preview(name = "History List 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun HistoryListLargeTextPreview() {
    AlcoholMeterTheme(true) { Surface { HistoryListScreen(previewHistory, {}, {}) } }
}
