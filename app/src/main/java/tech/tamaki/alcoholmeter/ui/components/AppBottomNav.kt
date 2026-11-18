package tech.tamaki.alcoholmeter.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

private data class NavEntry(val route: String, val label: Int, @DrawableRes val icon: Int)

private val navEntries = listOf(
    NavEntry("home", R.string.nav_home, R.drawable.ic_house),
    NavEntry("history", R.string.nav_history, R.drawable.ic_logs),
    NavEntry("calendar", R.string.nav_calendar, R.drawable.ic_calendar_days),
    NavEntry("settings", R.string.nav_settings, R.drawable.ic_sliders_horizontal),
)

@Composable
fun AppBottomNav(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dividerColor = MaterialTheme.colorScheme.outline
    Column(modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(dividerColor, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Offset(size.width, 0f))
        }
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            navEntries.forEach { entry ->
                val active = selected == entry.route
                val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                val label = stringResource(entry.label)
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(0.dp))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(entry.route) }
                        .semantics { contentDescription = label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter = painterResource(entry.icon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        colorFilter = ColorFilter.tint(tint),
                    )
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = tint)
                }
            }
        }
    }
}

@Preview(name = "AppBottomNav Light", showBackground = true)
@Composable
private fun NavLightPreview() {
    AlcoholMeterTheme(false) { AppBottomNav("home", {}) }
}

@Preview(name = "AppBottomNav Dark", showBackground = true)
@Composable
private fun NavDarkPreview() {
    AlcoholMeterTheme(true) { AppBottomNav("home", {}) }
}

@Preview(name = "AppBottomNav 200%", showBackground = true, fontScale = 2f)
@Composable
private fun NavLargeTextPreview() {
    AlcoholMeterTheme(true) { AppBottomNav("home", {}) }
}
