package tech.tamaki.alcoholmeter.ui.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun EndPartyConfirmation(
    state: EndPartyUiState,
    onConfirm: (EndTimeChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by remember(state.initialSelection) { mutableStateOf(state.initialSelection) }
    Box(Modifier.fillMaxSize().background(Color(0xB8050607))) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(20.dp),
        ) {
            Text(stringResource(R.string.end_question), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.end_dialog_body, state.cups, state.duration), Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Text(stringResource(R.string.end_time_label), Modifier.padding(top = 14.dp, bottom = 8.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EndTimeOption(stringResource(R.string.end_time_now, state.nowLabel), choice == EndTimeChoice.Now, Modifier.weight(1f)) { choice = EndTimeChoice.Now }
                EndTimeOption(stringResource(R.string.end_time_last_drink, state.lastDrinkLabel), choice == EndTimeChoice.LastDrink, Modifier.weight(1f)) { choice = EndTimeChoice.LastDrink }
            }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppButton(stringResource(R.string.still_drinking), onDismiss, Modifier.weight(1f), AppButtonStyle.Secondary, 56.dp)
                AppButton(stringResource(R.string.end_party), { onConfirm(choice) }, Modifier.weight(1f), AppButtonStyle.Primary, 56.dp)
            }
        }
    }
}

@Composable
private fun EndTimeOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState().value
    val shape = RoundedCornerShape(10.dp)
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        pressed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier.height(52.dp).clip(shape).background(background).border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = foreground, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Preview(name = "End Confirmation Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EndConfirmationLightPreview() {
    AlcoholMeterTheme(false) { EndPartyConfirmation(EndPartyUiState(5, "2時間42分", "21:46", "21:14", EndTimeChoice.Now), {}, {}) }
}

@Preview(name = "End Confirmation Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun EndConfirmationDarkPreview() {
    AlcoholMeterTheme(true) { EndPartyConfirmation(EndPartyUiState(5, "2時間42分", "21:46", "21:14", EndTimeChoice.LastDrink), {}, {}) }
}

@Preview(name = "End Confirmation 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun EndConfirmationLargeTextPreview() {
    AlcoholMeterTheme(true) { EndPartyConfirmation(EndPartyUiState(5, "2時間42分", "21:46", "21:14", EndTimeChoice.Now), {}, {}) }
}
