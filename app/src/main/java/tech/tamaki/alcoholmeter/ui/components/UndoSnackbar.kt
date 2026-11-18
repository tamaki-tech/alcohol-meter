package tech.tamaki.alcoholmeter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@Composable
fun UndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    resetKey: Any = message,
) {
    var remaining by remember(resetKey) { mutableFloatStateOf(1f) }
    LaunchedEffect(resetKey) {
        repeat(50) { tick ->
            delay(100)
            remaining = (49 - tick).coerceAtLeast(0) / 49f
        }
    }
    val background = MaterialTheme.colorScheme.onSurface
    val foreground = MaterialTheme.colorScheme.background
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(background),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, Modifier.weight(1f), color = foreground, fontSize = 14.sp)
            Box(
                Modifier.height(48.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onUndo).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.snackbar_undo),
                    color = foreground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(background.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(remaining).fillMaxHeight().background(foreground))
        }
    }
}

@Preview(name = "UndoSnackbar Light", showBackground = true)
@Composable
private fun UndoLightPreview() {
    AlcoholMeterTheme(false) { UndoSnackbar("ビールを記録（5杯目）", {}) }
}

@Preview(name = "UndoSnackbar Dark", showBackground = true)
@Composable
private fun UndoDarkPreview() {
    AlcoholMeterTheme(true) { UndoSnackbar("ビールを記録（5杯目）", {}) }
}

@Preview(name = "UndoSnackbar 200%", showBackground = true, fontScale = 2f)
@Composable
private fun UndoLargeTextPreview() {
    AlcoholMeterTheme(true) { UndoSnackbar("ビールを記録（5杯目）", {}) }
}
