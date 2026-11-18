package tech.tamaki.alcoholmeter.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.components.DrinkButton
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsDark
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsLight

private val beverageTypes = listOf("BEER" to 1, "HIGHBALL" to 18, "SAKE" to 10, "SHOCHU" to 11, "WINE" to 2, "CHUHAI" to 17, "COCKTAIL" to 16, "WHISKEY" to 5, "OTHER" to 0)

@Composable
fun PresetEditorScreen(
    state: PresetEditorUiState,
    draft: PresetEditorDraft,
    onDraftChange: (PresetEditorDraft) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val palette = if (dark) DrinkColorsDark else DrinkColorsLight
    val currentPreset = state.preset.copy(
        name = draft.name,
        shortName = draft.name,
        volumeMl = draft.volumeMl.toDoubleOrNull() ?: 0.0,
        abvPercent = draft.abvPercent.toDoubleOrNull() ?: 0.0,
        beverageType = draft.beverageType,
        colorIndex = draft.colorIndex,
        showInNotification = draft.notificationEnabled,
    )
    val pureAlcohol = ((draft.volumeMl.toDoubleOrNull() ?: 0.0) * (draft.abvPercent.toDoubleOrNull() ?: 0.0) / 100.0 * 0.8)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            AppButton("", onClose, Modifier.size(48.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_x, iconContentDescription = stringResource(R.string.preset_editor_close))
            Text(stringResource(R.string.preset_editor_title), Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            AppButton(stringResource(R.string.save), onSave, style = AppButtonStyle.Primary, height = 48.dp)
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
            DrinkButton(currentPreset.name, currentPreset.shortName, 0, currentPreset.colorIndex, iconResource(currentPreset.icon), {}, Modifier.width(160.dp), dark)
        }
        Column(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) {
            EditorField(stringResource(R.string.preset_name), draft.name, { onDraftChange(draft.copy(name = it)) }, KeyboardType.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) { EditorField(stringResource(R.string.preset_volume), draft.volumeMl, { onDraftChange(draft.copy(volumeMl = it)) }, KeyboardType.Decimal, stringResource(R.string.unit_ml)) }
                Column(Modifier.weight(1f)) { EditorField(stringResource(R.string.preset_abv), draft.abvPercent, { onDraftChange(draft.copy(abvPercent = it)) }, KeyboardType.Decimal, "%") }
            }
            Text(stringResource(R.string.preset_beverage_type), Modifier.padding(top = 14.dp, bottom = 6.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            beverageTypes.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (type, value) ->
                        val selected = draft.beverageType == value
                        AppButton(type, { onDraftChange(draft.copy(beverageType = value)) }, Modifier.weight(1f), if (selected) AppButtonStyle.Secondary else AppButtonStyle.Ghost, 48.dp)
                    }
                }
            }
            Text(stringResource(R.string.preset_color), Modifier.padding(top = 14.dp, bottom = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEachIndexed { index, color ->
                    Box(Modifier.size(44.dp).border(if (index == draft.colorIndex) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape).padding(2.dp).background(color, CircleShape).clickable { onDraftChange(draft.copy(colorIndex = index)) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.show_notification), fontSize = 14.sp)
                    Text(stringResource(R.string.preset_notification_max), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
                AppButton(if (draft.notificationEnabled) "ON" else "OFF", { if (!state.notificationLimitReached || draft.notificationEnabled) onDraftChange(draft.copy(notificationEnabled = !draft.notificationEnabled)) }, style = AppButtonStyle.Secondary, height = 48.dp)
            }
            if (state.notificationLimitReached && !draft.notificationEnabled) {
                Text(stringResource(R.string.preset_notification_limit_reason), Modifier.padding(top = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Text(stringResource(R.string.preset_pure_alcohol, pureAlcohol), Modifier.padding(top = 16.dp, bottom = 20.dp), fontSize = 14.sp)
        }
    }
}

@Composable
private fun EditorField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType, unit: String = "") {
    var focused = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(label, Modifier.padding(bottom = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        Row(Modifier.fillMaxWidth().height(48.dp).border(1.dp, if (focused.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(value, onValueChange, Modifier.weight(1f).onFocusChanged { focused.value = it.isFocused }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
            if (unit.isNotEmpty()) Text(unit, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
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

private val previewPreset = DrinkPreset(1, "ビール（中ジョッキ）", "ビール", 1, 350.0, 5.0, "beer", 0, 0, true)
private val previewEditor = PresetEditorUiState(previewPreset, "14.0 g", true, true)
private val previewDraft = PresetEditorDraft("ビール（中ジョッキ）", "350", "5", 1, 0, true)

@Preview(name = "Preset Editor Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PresetEditorLightPreview() { AlcoholMeterTheme(false) { Surface { PresetEditorScreen(previewEditor, previewDraft, {}, {}, {}) } } }

@Preview(name = "Preset Editor Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PresetEditorDarkPreview() { AlcoholMeterTheme(true) { Surface { PresetEditorScreen(previewEditor, previewDraft, {}, {}, {}) } } }

@Preview(name = "Preset Editor 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun PresetEditorLargeTextPreview() { AlcoholMeterTheme(true) { Surface { PresetEditorScreen(previewEditor, previewDraft, {}, {}, {}) } } }
