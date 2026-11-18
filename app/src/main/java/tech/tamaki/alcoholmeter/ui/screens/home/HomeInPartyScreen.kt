package tech.tamaki.alcoholmeter.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.domain.model.DrinkPreset
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.components.DrinkButton
import tech.tamaki.alcoholmeter.ui.components.DrinkColorDot
import tech.tamaki.alcoholmeter.ui.components.MetricStrip
import tech.tamaki.alcoholmeter.ui.components.MetricValue
import tech.tamaki.alcoholmeter.ui.components.SecondaryButton
import tech.tamaki.alcoholmeter.ui.components.UndoSnackbar
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@Composable
fun HomeInPartyScreen(
    state: HomeInPartyUiState,
    onRecordDrink: (DrinkPreset) -> Unit,
    onUndoDrink: () -> Unit,
    onEndParty: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val lastPreset = state.presets.firstOrNull { it.id == state.lastPresetId }
        ?: state.presets.firstOrNull()
    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.padding(end = 8.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
            Text(
                stringResource(R.string.home_in_party),
                Modifier.weight(1f),
                fontSize = 19.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
            Text(
                stringResource(R.string.home_started_at, state.startedAt),
                Modifier.padding(end = 8.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
            SecondaryButton(stringResource(R.string.end_party), onEndParty, Modifier.height(48.dp))
        }
        MetricStrip(
            elapsed = MetricValue(stringResource(R.string.metric_elapsed), state.elapsed),
            cups = MetricValue(stringResource(R.string.metric_cups), state.cups.toString(), stringResource(R.string.unit_cups)),
            bac = MetricValue(stringResource(R.string.metric_bac), state.bac.removeSuffix("%"), "%", true),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.background),
            cupCount = state.cups,
        )
        Text(
            stringResource(R.string.home_pure_alcohol_and_caution, state.pureAlcohol, stringResource(R.string.bac_caution)),
            Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            stringResource(R.string.home_recent_three),
            Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        state.recentDrinks.take(3).forEach { drink ->
            Row(
                Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrinkColorDot(drink.colorIndex)
                Text(
                    drink.time,
                    Modifier.padding(start = 8.dp).width(44.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
                Text(drink.name, fontSize = 14.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.weight(1f))
        if (lastPreset != null) {
            AppButton(
                text = stringResource(R.string.home_another_drink, lastPreset.name),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRecordDrink(lastPreset)
                },
                modifier = Modifier.fillMaxWidth().height(112.dp).padding(horizontal = 16.dp),
                style = AppButtonStyle.Primary,
                height = 112.dp,
                content = {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.home_another_drink_label), fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(iconResource(lastPreset.icon)),
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                            )
                            Text(lastPreset.name, Modifier.padding(start = 8.dp), fontSize = 30.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                },
                contentDescription = stringResource(R.string.home_another_drink_accessibility, lastPreset.name),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.presets, key = { it.id }) { preset ->
                DrinkButton(
                    name = preset.name,
                    shortName = preset.shortName,
                    count = state.drinkCounts[preset.id] ?: 0,
                    colorIndex = preset.colorIndex,
                    icon = iconResource(preset.icon),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRecordDrink(preset)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
      }
      state.undoMessage?.let { message ->
          UndoSnackbar(message = message, onUndo = onUndoDrink, modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp).padding(bottom = 76.dp), resetKey = message)
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

private val previewPresets = listOf(
    DrinkPreset(1, "ビール（中ジョッキ）", "ビール", 1, 350.0, 5.0, "beer", 0, 0, true),
    DrinkPreset(2, "ハイボール", "ハイボール", 2, 350.0, 7.0, "glass-water", 1, 1, true),
    DrinkPreset(3, "日本酒（1合）", "日本酒", 3, 180.0, 15.0, "milk", 2, 2, true),
    DrinkPreset(4, "ワイン（グラス）", "ワイン", 4, 120.0, 12.0, "wine", 3, 3, false),
    DrinkPreset(5, "レモンサワー", "レモン", 5, 350.0, 5.0, "citrus", 4, 4, false),
    DrinkPreset(6, "焼酎ロック", "焼酎", 6, 60.0, 25.0, "sho-chu", 5, 5, false),
)

private val previewState = HomeInPartyUiState(
    startedAt = "19:04",
    elapsed = "1:42:18",
    cups = 4,
    bac = "0.045%",
    pureAlcohol = "52.0 g",
    recentDrinks = listOf(
        HomeRecentDrinkUiState("レモンサワー", "21:14", 4),
        HomeRecentDrinkUiState("ビール（中ジョッキ）", "20:42", 0),
        HomeRecentDrinkUiState("ハイボール", "20:10", 1),
    ),
    presets = previewPresets,
    drinkCounts = mapOf(1L to 1, 2L to 1, 5L to 2),
    lastPresetId = 5,
    undoMessage = "レモンサワーを記録（5杯目）",
)

@Preview(name = "Home In Party Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeInPartyLightPreview() {
    AlcoholMeterTheme(false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeInPartyScreen(previewState, {}, {}, {})
        }
    }
}

@Preview(name = "Home In Party Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeInPartyDarkPreview() {
    AlcoholMeterTheme(true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeInPartyScreen(previewState, {}, {}, {})
        }
    }
}

@Preview(name = "Home In Party 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun HomeInPartyLargeTextPreview() {
    AlcoholMeterTheme(true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeInPartyScreen(previewState, {}, {}, {})
        }
    }
}
