package tech.tamaki.alcoholmeter.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onWeightChange: (String) -> Unit,
    onSexChange: (OnboardingSex) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onRequestHealthConnect: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                Box(Modifier.weight(1f).height(3.dp).background(if (index < state.step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)))
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 36.dp)) {
            when (state.step) {
                1 -> WeightAndSexStep(state, onWeightChange, onSexChange)
                2 -> HealthConnectStep(state.healthConnectGranted)
                3 -> NotificationStep()
                else -> WeightAndSexStep(state, onWeightChange, onSexChange)
            }
        }
        when (state.step) {
            2 -> {
                AppButton(stringResource(R.string.onboarding_hc_allow), onRequestHealthConnect, Modifier.fillMaxWidth(), AppButtonStyle.Primary, 56.dp)
                AppButton(stringResource(R.string.onboarding_skip_link), onSkip, Modifier.fillMaxWidth(), AppButtonStyle.Ghost, 48.dp)
            }
            3 -> {
                AppButton(stringResource(R.string.onboarding_notification_allow), onRequestNotifications, Modifier.fillMaxWidth(), AppButtonStyle.Primary, 56.dp)
                AppButton(stringResource(R.string.onboarding_later), onSkip, Modifier.fillMaxWidth(), AppButtonStyle.Ghost, 48.dp)
            }
            else -> AppButton(stringResource(R.string.onboarding_next), onContinue, Modifier.fillMaxWidth(), AppButtonStyle.Primary, 56.dp, enabled = state.sex != null && state.weightKg.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true)
        }
    }
}

@Composable
private fun WeightAndSexStep(state: OnboardingUiState, onWeightChange: (String) -> Unit, onSexChange: (OnboardingSex) -> Unit) {
    Text(stringResource(R.string.onboarding_weight_title), fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.onboarding_weight_subtitle), Modifier.padding(top = 12.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    state.healthConnectWeightDate?.let { date ->
        Text(stringResource(R.string.onboarding_weight_source, date), Modifier.padding(top = 14.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
    }
    Row(Modifier.padding(top = 30.dp), verticalAlignment = Alignment.Bottom) {
        BasicTextField(
            value = state.weightKg,
            onValueChange = onWeightChange,
            modifier = Modifier.weight(1f).padding(bottom = 8.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        )
        Text(stringResource(R.string.unit_kg), Modifier.padding(bottom = 14.dp), fontSize = 18.sp, color = MaterialTheme.colorScheme.secondary)
    }
    Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.primary))
    Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SexOption(OnboardingSex.Male, state.sex, onSexChange, Modifier.weight(1f))
        SexOption(OnboardingSex.Female, state.sex, onSexChange, Modifier.weight(1f))
    }
}

@Composable
private fun SexOption(option: OnboardingSex, selected: OnboardingSex?, onSelect: (OnboardingSex) -> Unit, modifier: Modifier) {
    AppButton(
        stringResource(if (option == OnboardingSex.Male) R.string.onboarding_male else R.string.onboarding_female),
        { onSelect(option) },
        modifier,
        if (option == selected) AppButtonStyle.Primary else AppButtonStyle.Secondary,
        56.dp,
    )
}

@Composable
private fun HealthConnectStep(granted: Boolean) {
    Text(stringResource(R.string.onboarding_hc_title), fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.onboarding_hc_subtitle), Modifier.padding(top = 12.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.onboarding_hc_write), fontSize = 16.sp)
        Text(stringResource(R.string.onboarding_hc_read), fontSize = 16.sp)
    }
    if (granted) Text(stringResource(R.string.onboarding_hc_granted), Modifier.padding(top = 18.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
}

@Composable
private fun NotificationStep() {
    Text(stringResource(R.string.onboarding_notification_title), fontSize = 32.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.onboarding_notification_subtitle), Modifier.padding(top = 12.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    Column(Modifier.fillMaxWidth().padding(top = 28.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_in_party), Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("1:42:18", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp)
        }
        Text(stringResource(R.string.onboarding_notification_sample), Modifier.padding(top = 8.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

private val previewState = OnboardingUiState(1, "68.0", OnboardingSex.Male, "11/12", false)

@Preview(name = "Onboarding Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingLightPreview() { AlcoholMeterTheme(false) { Surface { OnboardingScreen(previewState, {}, {}, {}, {}, {}, {}) } } }

@Preview(name = "Onboarding Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingDarkPreview() { AlcoholMeterTheme(true) { Surface { OnboardingScreen(previewState, {}, {}, {}, {}, {}, {}) } } }

@Preview(name = "Onboarding 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun OnboardingLargeTextPreview() { AlcoholMeterTheme(true) { Surface { OnboardingScreen(previewState, {}, {}, {}, {}, {}, {}) } } }
