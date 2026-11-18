package tech.tamaki.alcoholmeter.ui.screens.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@Composable
fun HomeBeforeScreen(
    state: HomeBeforeUiState,
    onStartParty: () -> Unit,
    onOpenRecentParty: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            stringResource(R.string.home_today),
            Modifier.padding(start = 4.dp, top = 16.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            state.date,
            Modifier.padding(start = 4.dp, top = 2.dp),
            fontSize = 24.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
        if (state.previousBac != null) {
            PreviousAlcoholCard(state)
        } else {
            RestDayCard(state)
        }
        state.recentParty?.let { recent ->
        Text(
            stringResource(R.string.home_recent_party),
            Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Row(
            Modifier.fillMaxWidth()
                .height(68.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenRecentParty)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(recent, Modifier.weight(1f), fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.home_chevron_description), fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
        }
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.home_start_hint),
            Modifier.fillMaxWidth().padding(bottom = 14.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        AppButton(
            text = stringResource(R.string.home_cheers),
            onClick = onStartParty,
            modifier = Modifier.fillMaxWidth().height(112.dp),
            style = AppButtonStyle.Primary,
            height = 112.dp,
            icon = R.drawable.ic_beer,
            iconContentDescription = stringResource(R.string.icon_beer_description),
            contentDescription = stringResource(R.string.home_cheers),
        )
        Spacer(Modifier.height(64.dp))
    }
}

@Composable
private fun RestDayCard(state: HomeBeforeUiState) {
    Column(
        Modifier.fillMaxWidth()
            .padding(top = 18.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                state.streakDays.toString(),
                fontSize = 96.sp,
                lineHeight = 86.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.home_rest_streak_suffix),
                Modifier.padding(start = 6.dp, bottom = 12.dp),
                fontSize = 24.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            )
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(stringResource(R.string.home_longest_streak, state.longestStreakDays), fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
            Text(stringResource(R.string.home_month_rest_days, state.monthRestDays), fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun PreviousAlcoholCard(state: HomeBeforeUiState) {
    Column(
        Modifier.fillMaxWidth()
            .padding(top = 18.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(stringResource(R.string.home_previous_alcohol), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(state.previousBac.orEmpty(), fontSize = 64.sp, lineHeight = 68.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text("%", Modifier.padding(start = 4.dp, bottom = 10.dp), fontSize = 18.sp)
        }
        Text(stringResource(R.string.home_sober_eta, state.soberEta.orEmpty()), fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Text(stringResource(R.string.bac_caution), Modifier.padding(top = 8.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
        Text(stringResource(R.string.home_streak_if_rest), Modifier.padding(top = 14.dp), fontSize = 14.sp)
    }
}

@Preview(name = "Home Before Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeBeforeLightPreview() {
    AlcoholMeterTheme(false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeBeforeScreen(HomeBeforeUiState("11月18日（水）", 12, 28, 9, "11/14（金） 19:02–23:40 ・ 7 杯 ・ 114.8 g"), {}, {})
        }
    }
}

@Preview(name = "Home Before Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HomeBeforeDarkPreview() {
    AlcoholMeterTheme(true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeBeforeScreen(HomeBeforeUiState("11月18日（水）", 12, 28, 9, "11/14（金） 19:02–23:40 ・ 7 杯 ・ 114.8 g"), {}, {})
        }
    }
}

@Preview(name = "Home Before 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun HomeBeforeLargeTextPreview() {
    AlcoholMeterTheme(true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeBeforeScreen(HomeBeforeUiState("11月18日（水）", 12, 28, 9, "11/14（金） 19:02–23:40 ・ 7 杯 ・ 114.8 g"), {}, {})
        }
    }
}
