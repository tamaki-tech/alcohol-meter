package tech.tamaki.alcoholmeter.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsDark
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsLight

@Composable
fun SettingsScreen(state: SettingsUiState, onEditPreset: (DrinkPreset) -> Unit, onAddPreset: () -> Unit, onReorderPreset: (Long, Long) -> Unit, onEditProfile: () -> Unit = {}, onHealthConnect: () -> Unit = {}) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = if (dark) DrinkColorsDark else DrinkColorsLight
    var draggingPreset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.nav_settings), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.presets), Modifier.weight(1f), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.settings_notification_count, state.notificationCount), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        state.presets.forEachIndexed { index, preset ->
            Row(
                // 行のどこをタップしても編集を開く（長押し→ドラッグは並べ替え）
                Modifier.fillMaxWidth().height(52.dp).clickable { onEditPreset(preset) }.pointerInput(state.presets) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { draggingPreset = preset.id },
                        onDragEnd = { draggingPreset = null },
                        onDragCancel = { draggingPreset = null },
                    ) { change, dragAmount ->
                        change.consume()
                        val move = when { dragAmount.y > 24f -> 1; dragAmount.y < -24f -> -1; else -> 0 }
                        val target = (index + move).coerceIn(0, state.presets.lastIndex)
                        if (move != 0 && target != index) onReorderPreset(preset.id, state.presets[target].id)
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.ic_grip_vertical), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary))
                Image(painterResource(iconResource(preset.icon)), null, Modifier.padding(start = 8.dp, end = 10.dp).size(22.dp), colorFilter = ColorFilter.tint(palette[preset.colorIndex.coerceIn(0, 5)]))
                Column(Modifier.weight(1f).then(if (draggingPreset == preset.id) Modifier.border(1.dp, MaterialTheme.colorScheme.primary) else Modifier)) {
                    Text(preset.name, fontSize = 14.sp, maxLines = 1)
                    Text(stringResource(R.string.settings_preset_volume, preset.volumeMl.toInt(), preset.abvPercent), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
                Image(painterResource(R.drawable.ic_bell), null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(if (preset.showInNotification) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)))
                AppButton("", { onEditPreset(preset) }, Modifier.size(48.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_pencil, iconContentDescription = stringResource(R.string.settings_edit_preset, preset.name))
            }
        }
        AppButton(stringResource(R.string.add_preset), onAddPreset, Modifier.fillMaxWidth().padding(top = 8.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_plus, iconContentDescription = stringResource(R.string.add_preset))
        Text(stringResource(R.string.settings_bac_calculation), Modifier.padding(top = 22.dp, bottom = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        SettingValueRow(stringResource(R.string.settings_weight), state.weight, onEditProfile)
        SettingValueRow(stringResource(R.string.settings_sex), state.sex, onEditProfile)
        SettingValueRow(stringResource(R.string.settings_elimination_rate), state.eliminationRate, onEditProfile)
        SettingValueRow(stringResource(R.string.settings_health_connect), state.healthConnect, onHealthConnect)
        SettingValueRow(stringResource(R.string.settings_day_cutoff), stringResource(R.string.calendar_cutoff))
    }
}

@Composable
private fun SettingValueRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(52.dp).then(if (onClick == null) Modifier else Modifier.clickable { onClick() }), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp)
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
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

private val previewPresets = listOf(DrinkPreset(1, "ビール（中ジョッキ）", "ビール", 1, 350.0, 5.0, "beer", 0, 0, true), DrinkPreset(2, "ハイボール", "ハイボール", 2, 350.0, 7.0, "glass-water", 1, 1, true))
private val previewState = SettingsUiState(previewPresets, 2, "68.0 kg", "男性", "0.015 %/時", "許可済み")

@Preview(name = "Settings Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsLightPreview() { AlcoholMeterTheme(false) { Surface { SettingsScreen(previewState, {}, {}, { _, _ -> }) } } }

@Preview(name = "Settings Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SettingsDarkPreview() { AlcoholMeterTheme(true) { Surface { SettingsScreen(previewState, {}, {}, { _, _ -> }) } } }

@Preview(name = "Settings 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun SettingsLargeTextPreview() { AlcoholMeterTheme(true) { Surface { SettingsScreen(previewState, {}, {}, { _, _ -> }) } } }
