package tech.tamaki.alcoholmeter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsDark
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsLight

@Composable
fun DrinkColorDot(
    colorIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
) {
    val palette = if (darkTheme) DrinkColorsDark else DrinkColorsLight
    Box(modifier.size(size).background(palette[colorIndex.coerceIn(0, palette.lastIndex)], CircleShape))
}

@Preview(name = "DrinkColorDot Light", showBackground = true)
@Composable
private fun DotLightPreview() {
    AlcoholMeterTheme(false) { DrinkColorDot(0, darkTheme = false) }
}

@Preview(name = "DrinkColorDot Dark", showBackground = true)
@Composable
private fun DotDarkPreview() {
    AlcoholMeterTheme(true) { DrinkColorDot(0, darkTheme = true) }
}

@Preview(name = "DrinkColorDot 200%", showBackground = true, fontScale = 2f)
@Composable
private fun DotLargeTextPreview() {
    AlcoholMeterTheme(true) { DrinkColorDot(0, darkTheme = true) }
}
