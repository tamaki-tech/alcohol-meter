package tech.tamaki.alcoholmeter.ui.screens.summary

import androidx.compose.foundation.border
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@Composable
fun SessionSummaryScreen(
    state: SummaryUiState,
    onClose: () -> Unit,
    onRetrySync: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.summary_greeting), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        Text(state.dateTitle, Modifier.padding(top = 2.dp), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        SessionSummaryContent(state)
        SyncStatusCard(state, onRetrySync, onOpenSettings)
        Spacer(Modifier.height(16.dp))
        AppButton(stringResource(R.string.close), onClose, Modifier.fillMaxWidth(), AppButtonStyle.Secondary, 56.dp)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun SyncStatusCard(state: SummaryUiState, onRetry: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 18.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            when (state.syncStatus) {
                HealthSyncUiStatus.Sent -> Text(stringResource(R.string.sync_sent, state.syncedCount), fontSize = 14.sp)
                HealthSyncUiStatus.Failed -> Text(stringResource(R.string.sync_failed), fontSize = 14.sp)
                HealthSyncUiStatus.NotLinked -> Text(stringResource(R.string.sync_not_linked), fontSize = 14.sp)
            }
        }
        when (state.syncStatus) {
            HealthSyncUiStatus.Failed -> AppButton(stringResource(R.string.retry), onRetry, style = AppButtonStyle.Ghost, height = 48.dp)
            HealthSyncUiStatus.NotLinked -> AppButton(stringResource(R.string.sync_open_settings), onSettings, style = AppButtonStyle.Ghost, height = 48.dp)
            HealthSyncUiStatus.Sent -> Unit
        }
    }
}

private val previewSummary = SummaryUiState(
    dateTitle = "11/18（水）の飲み会",
    metrics = listOf(
        SessionMetricUiState("開始", "19:02"),
        SessionMetricUiState("終了", "23:40"),
        SessionMetricUiState("所要時間", "4時間38分"),
        SessionMetricUiState("純アルコール", "114.8 g"),
    ),
    categories = listOf(DrinkCategoryUiState("ビール", 2, 0), DrinkCategoryUiState("ハイボール", 3, 1)),
    bacPoints = (0..72).map { index -> BacPointUiState("${19 + index / 12}:${(index % 12) * 5}", (0.065f - index * 0.0008f).coerceAtLeast(0f), if (index % 12 == 0) index % 6 else null) },
    maximumBac = "0.067%",
    soberEta = "00:55",
    drinkRows = listOf(DrinkRowUiState("19:20", "ビール（中ジョッキ）", "14.0 g", 0), DrinkRowUiState("20:05", "ハイボール", "19.6 g", 1)),
    syncStatus = HealthSyncUiStatus.Sent,
    syncedCount = 5,
)

@Preview(name = "Summary Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryLightPreview() {
    AlcoholMeterTheme(false) { Surface { SessionSummaryScreen(previewSummary, {}, {}, {}) } }
}

@Preview(name = "Summary Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SummaryDarkPreview() {
    AlcoholMeterTheme(true) { Surface { SessionSummaryScreen(previewSummary, {}, {}, {}) } }
}

@Preview(name = "Summary 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun SummaryLargeTextPreview() {
    AlcoholMeterTheme(true) { Surface { SessionSummaryScreen(previewSummary, {}, {}, {}) } }
}
