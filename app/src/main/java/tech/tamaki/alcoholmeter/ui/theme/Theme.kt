package tech.tamaki.alcoholmeter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(primary=DarkOnSurface, onPrimary=DarkBackground, background=DarkBackground, onBackground=DarkOnSurface, surface=DarkSurface, onSurface=DarkOnSurface, outline=DarkDivider, secondary=DarkSecondary)
private val LightScheme = lightColorScheme(primary=LightPrimary, onPrimary=Color.White, background=LightBackground, onBackground=LightOnSurface, surface=LightBackground, onSurface=LightOnSurface, outline=LightDivider, secondary=LightMuted)

@Composable
fun AlcoholMeterTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(colorScheme = colorScheme, typography = AlcoholTypography, shapes = AlcoholShapes) {
        // ルートに背景色と文字色を敷く。これが無いと LocalContentColor が既定の黒のままになり、ダークで文字が読めない
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
            contentColor = colorScheme.onBackground,
            content = content,
        )
    }
}
