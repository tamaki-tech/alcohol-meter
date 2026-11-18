package tech.tamaki.alcoholmeter.ui.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.components.DrinkButton
import tech.tamaki.alcoholmeter.ui.components.DrinkColorDot
import tech.tamaki.alcoholmeter.ui.screens.summary.DrinkCategoryUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.DrinkRowUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.BacPointUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.HealthSyncUiStatus
import tech.tamaki.alcoholmeter.ui.screens.summary.SessionMetricUiState
import tech.tamaki.alcoholmeter.ui.screens.summary.SessionSummaryContent
import tech.tamaki.alcoholmeter.ui.screens.summary.SummaryUiState
import java.time.LocalDateTime
import tech.tamaki.alcoholmeter.ui.screens.summary.SyncStatusCard
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsDark

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HistoryDetailScreen(
    state: HistoryDetailUiState,
    onBack: () -> Unit,
    onEditDrink: (String) -> Unit,
    onSaveEdit: (DrinkEditUiState) -> Unit,
    onDeleteDrink: () -> Unit,
    onDeleteSession: () -> Unit,
    onRetrySync: () -> Unit,
    onOpenSettings: () -> Unit,
    onCancelEdit: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSessionDelete by remember { mutableStateOf(false) }
    var showDrinkDelete by remember { mutableStateOf(false) }
    var editState by remember(state.edit) { mutableStateOf(state.edit) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppButton("", onBack, Modifier.size(48.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_chevron_left, iconContentDescription = stringResource(R.string.history_back))
            Text(state.summary.dateTitle, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Box {
                AppButton("", { showMenu = true }, Modifier.size(48.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_ellipsis_vertical, iconContentDescription = stringResource(R.string.history_menu))
                // メニューのアイコンからいきなり削除確認を出さず、何をするメニューかを見せてから確認する
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_delete_session)) },
                        onClick = { showMenu = false; showSessionDelete = true },
                        leadingIcon = { Image(painterResource(R.drawable.ic_trash), null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.history_detail_overview, state.startTime, state.endTime, state.summary.drinkRows.size, state.summary.metrics.lastOrNull()?.value.orEmpty()),
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SessionSummaryContent(state.summary, onEditDrink)
            SyncStatusCard(state.summary, onRetrySync, onOpenSettings)
        }
    }
    editState?.let { edit ->
        EditDrinkBottomSheet(edit, { onSaveEdit(edit) }, { showDrinkDelete = true }, { editState = null; onCancelEdit() }, { id -> editState = edit.copy(selectedPresetId = id) }) { hour, minute ->
            val selected = edit.selectedDateTime.withHour(hour).withMinute(minute)
            editState = edit.copy(time = "%02d:%02d".format(hour, minute), selectedDateTime = selected)
        }
    }
    if (showDrinkDelete) { ConfirmDeleteDialog(stringResource(R.string.history_delete_drink_question), { showDrinkDelete = false; onDeleteDrink() }) { showDrinkDelete = false } }
    if (showSessionDelete) {
        ConfirmDeleteDialog(stringResource(R.string.history_delete_session_question), onDeleteSession) { showSessionDelete = false }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditDrinkBottomSheet(
    state: DrinkEditUiState,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onPresetSelected: (Long) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timeParts = state.time.split(":")
    val pickerState = rememberTimePickerState(timeParts.getOrNull(0)?.toIntOrNull() ?: 0, timeParts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true)
    // 戻る操作・scrim のタップはシートを閉じる（画面ごと戻ったり、裏の行をタップしたりしない）
    BackHandler(onBack = onCancel)
    Box(Modifier.fillMaxSize().background(Color(0x99050607)).pointerInput(onCancel) { detectTapGestures(onTap = { onCancel() }) }, contentAlignment = Alignment.BottomCenter) {
        Column(
            Modifier.fillMaxWidth().pointerInput(Unit) { detectTapGestures { } }.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Box(Modifier.width(36.dp).height(4.dp).background(MaterialTheme.colorScheme.outline, CircleShape).align(Alignment.CenterHorizontally))
            Text(state.title, Modifier.padding(vertical = 16.dp), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().height(108.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.presets, key = { it.id }) { preset ->
                    val selected = state.selectedPresetId == preset.id
                    AppButton(
                        text = "${preset.shortName}",
                        onClick = { onPresetSelected(preset.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).border(if (selected) 1.dp else 0.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        style = if (selected) AppButtonStyle.Secondary else AppButtonStyle.Ghost,
                        height = 48.dp,
                        icon = iconResource(preset.icon),
                        iconContentDescription = preset.name,
                    )
                }
            }
            AppButton(
                stringResource(R.string.history_time_value, state.time, state.date),
                { showTimePicker = true },
                Modifier.fillMaxWidth().padding(top = 12.dp),
                AppButtonStyle.Secondary,
                52.dp,
                icon = R.drawable.ic_calendar_days,
                iconContentDescription = stringResource(R.string.history_choose_time),
            )
            Text(stringResource(R.string.edit_sync_note), Modifier.padding(top = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            if (!state.timeWithinSession) {
                Text(stringResource(R.string.history_time_out_of_range), Modifier.padding(top = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppButton(stringResource(R.string.delete), onDelete, style = AppButtonStyle.Ghost)
                Spacer(Modifier.weight(1f))
                AppButton(stringResource(R.string.cancel), onCancel, style = AppButtonStyle.Secondary)
                AppButton(stringResource(R.string.save), onSave, style = AppButtonStyle.Primary, enabled = state.timeWithinSession)
            }
        }
    }
    if (showTimePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.history_choose_time)) },
            text = { TimePicker(pickerState) },
            confirmButton = { AppButton(stringResource(R.string.save), { onTimeSelected(pickerState.hour, pickerState.minute); showTimePicker = false }, style = AppButtonStyle.Primary) },
            dismissButton = { AppButton(stringResource(R.string.cancel), { showTimePicker = false }, style = AppButtonStyle.Ghost) },
        )
    }
}

@Composable
fun ConfirmDeleteDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(20.dp)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), AppButtonStyle.Secondary)
                AppButton(stringResource(R.string.delete), onConfirm, Modifier.weight(1f), AppButtonStyle.Primary)
            }
        }
    }
}

private fun iconResource(icon: String): Int = when (icon) {
    "beer" -> R.drawable.ic_beer
    "glass-water" -> R.drawable.ic_glass_water
    "milk" -> R.drawable.ic_milk
    "wine" -> R.drawable.ic_wine
    "citrus" -> R.drawable.ic_citrus
    else -> R.drawable.ic_shochu_glass
}

private val detailSummary = SummaryUiState(
    "11/14（金）の飲み会",
    listOf(SessionMetricUiState("開始", "19:02"), SessionMetricUiState("終了", "23:40"), SessionMetricUiState("所要時間", "4時間38分"), SessionMetricUiState("純アルコール", "114.8 g")),
    listOf(DrinkCategoryUiState("ビール", 2, 0)),
    (0..48).map { BacPointUiState("19:${(it * 5) % 60}", (0.06f - it * 0.001f).coerceAtLeast(0f), if (it % 12 == 0) 0 else null) },
    "0.067%",
    "00:55",
    listOf(DrinkRowUiState("19:20", "ビール（中ジョッキ）", "14.0 g", 0), DrinkRowUiState("20:05", "ハイボール", "19.6 g", 1)),
    HealthSyncUiStatus.Sent,
    5,
)

private val detailPresets = listOf(DrinkPreset(1, "ビール", "ビール", 1, 350.0, 5.0, "beer", 0, 0, true), DrinkPreset(2, "ハイボール", "ハイボール", 2, 350.0, 7.0, "glass-water", 1, 1, true))
private val detailState = HistoryDetailUiState(detailSummary, "19:02", "23:40", DrinkEditUiState("1 杯を編集", 1, "20:05", "11/14", detailPresets, java.time.LocalDateTime.of(2026, 11, 14, 19, 2), java.time.LocalDateTime.of(2026, 11, 14, 23, 40), java.time.LocalDateTime.of(2026, 11, 14, 20, 5)))

@Preview(name = "History Detail Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HistoryDetailLightPreview() {
    AlcoholMeterTheme(false) { Surface { HistoryDetailScreen(detailState, {}, {}, {}, {}, {}, {}, {}) } }
}

@Preview(name = "History Detail Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HistoryDetailDarkPreview() {
    AlcoholMeterTheme(true) { Surface { HistoryDetailScreen(detailState.copy(edit = null), {}, {}, {}, {}, {}, {}, {}) } }
}

@Preview(name = "History Detail 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun HistoryDetailLargeTextPreview() {
    AlcoholMeterTheme(true) { Surface { HistoryDetailScreen(detailState, {}, {}, {}, {}, {}, {}, {}) } }
}
