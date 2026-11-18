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

private val DarkScheme = darkColorScheme(primary=DarkOnSurface, onPrimary=DarkBackground, background=DarkBackground, onBackground=DarkOnSurface, surface=DarkSurface, onSurface=DarkOnSurface, outline=DarkDivider, secondary=DarkSecondary, tertiary=DarkRestDay,
    // Material 3 の部品（ダイアログ・メニュー・TimePicker）が既定の紫系で描かれないよう、アプリの配色で埋める
    surfaceContainerLowest=DarkSurface, surfaceContainerLow=DarkSurface, surfaceContainer=DarkSurface, surfaceContainerHigh=DarkSurface, surfaceContainerHighest=DarkDivider,
    primaryContainer=Color(0xFF3F3F46), onPrimaryContainer=DarkOnSurface, surfaceVariant=DarkDivider, onSurfaceVariant=DarkSecondary)
private val LightScheme = lightColorScheme(primary=LightPrimary, onPrimary=Color.White, background=LightBackground, onBackground=LightOnSurface, surface=LightBackground, onSurface=LightOnSurface, outline=LightDivider, secondary=LightMuted, tertiary=LightRestDay,
    surfaceContainerLowest=LightBackground, surfaceContainerLow=LightBackground, surfaceContainer=LightBackground, surfaceContainerHigh=LightBackground, surfaceContainerHighest=Color(0xFFF4F4F5),
    primaryContainer=LightDivider, onPrimaryContainer=LightOnSurface, surfaceVariant=Color(0xFFF4F4F5), onSurfaceVariant=LightMuted)

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
