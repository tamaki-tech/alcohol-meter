package tech.tamaki.alcoholmeter.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsDark
import tech.tamaki.alcoholmeter.ui.theme.DrinkColorsLight

@Composable
fun DrinkButton(
    name: String,
    shortName: String,
    count: Int,
    colorIndex: Int,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
) {
    val palette = if (darkTheme) DrinkColorsDark else DrinkColorsLight
    val drinkColor = palette[colorIndex.coerceIn(0, palette.lastIndex)]
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    val pressTint = MaterialTheme.colorScheme.onSurface.copy(if (darkTheme) 0.10f else 0.06f)
    val recordDescription = stringResource(R.string.drink_record_description, name)
    Column(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (pressed) pressTint else Color.Transparent)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics { contentDescription = recordDescription }
            .padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp), colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(drinkColor))
            Spacer(Modifier.weight(1f))
            if (count > 0) Text("×$count", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(shortName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.fillMaxWidth().height(3.dp).background(drinkColor))
    }
}

@Preview(name = "DrinkButton Light", showBackground = true)
@Composable
private fun DrinkButtonLightPreview() {
    AlcoholMeterTheme(false) { DrinkButton("ビール（中ジョッキ）", "ビール", 2, 0, R.drawable.ic_beer, {}, darkTheme = false) }
}

@Preview(name = "DrinkButton Dark", showBackground = true)
@Composable
private fun DrinkButtonDarkPreview() {
    AlcoholMeterTheme(true) { DrinkButton("ビール（中ジョッキ）", "ビール", 2, 0, R.drawable.ic_beer, {}, darkTheme = true) }
}

@Preview(name = "DrinkButton 200%", showBackground = true, fontScale = 2f)
@Composable
private fun DrinkButtonLargeTextPreview() {
    AlcoholMeterTheme(true) { DrinkButton("ビール（中ジョッキ）", "ビール", 2, 0, R.drawable.ic_beer, {}, darkTheme = true) }
}
