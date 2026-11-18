package tech.tamaki.alcoholmeter.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme
import tech.tamaki.alcoholmeter.ui.theme.DarkPrimaryPressed
import tech.tamaki.alcoholmeter.ui.theme.LightPrimaryPressed

enum class AppButtonStyle { Primary, Secondary, Ghost }

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    height: Dp = 48.dp,
    @DrawableRes icon: Int? = null,
    iconContentDescription: String? = null,
    contentDescription: String? = null,
    enabled: Boolean = true,
    iconAlpha: Float = 1f,
    content: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    val scheme = MaterialTheme.colorScheme
    val background = when {
        pressed && style == AppButtonStyle.Primary -> if (scheme.background == Color(0xFF09090B)) DarkPrimaryPressed else LightPrimaryPressed
        pressed -> scheme.onSurface.copy(alpha = if (scheme.background == Color.White) 0.06f else 0.10f)
        style == AppButtonStyle.Primary -> scheme.primary
        else -> Color.Transparent
    }
    val foreground = if (style == AppButtonStyle.Primary) scheme.onPrimary else scheme.onSurface
    val shape = RoundedCornerShape(if (height == 112.dp) 14.dp else 10.dp)
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp)
            .height(height)
            // 無効なボタンは薄く表示し、押せないことを見て分かるようにする
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(background)
            .then(if (style == AppButtonStyle.Secondary) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            // アイコンだけのボタンは余白を取らない（48dp から余白を引くとアイコンが縮んでしまう）
            .padding(horizontal = if (text.isEmpty() && icon != null && content == null) 0.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (content != null) {
            // 独自レイアウトにもボタンの文字色を伝える（塗りボタン上で黒文字にならないように）
            CompositionLocalProvider(LocalContentColor provides foreground) { content() }
        } else {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = iconContentDescription,
                    modifier = Modifier.size(24.dp),
                    colorFilter = ColorFilter.tint(foreground.copy(alpha = iconAlpha)),
                )
                if (text.isNotEmpty()) Spacer(Modifier.size(8.dp))
            }
            if (text.isNotEmpty()) Text(text, color = foreground, fontSize = if (style == AppButtonStyle.Primary && height == 112.dp) 40.sp else 16.sp)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, large: Boolean = false) {
    AppButton(text, onClick, modifier, AppButtonStyle.Primary, if (large) 56.dp else 48.dp)
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppButton(text, onClick, modifier, AppButtonStyle.Secondary)
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppButton(text, onClick, modifier, AppButtonStyle.Ghost)
}

@Preview(name = "AppButton Light", showBackground = true)
@Composable
private fun AppButtonLightPreview() {
    AlcoholMeterTheme(false) { AppButton(stringResource(R.string.preview_button), {}, Modifier.padding(16.dp)) }
}

@Preview(name = "AppButton Dark", showBackground = true)
@Composable
private fun AppButtonDarkPreview() {
    AlcoholMeterTheme(true) { AppButton(stringResource(R.string.preview_button), {}, Modifier.padding(16.dp)) }
}

@Preview(name = "AppButton 200%", showBackground = true, fontScale = 2f)
@Composable
private fun AppButtonLargeTextPreview() {
    AlcoholMeterTheme(true) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
            AppButton(stringResource(R.string.preview_button), {}, Modifier.padding(16.dp))
        }
    }
}
