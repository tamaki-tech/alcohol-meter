package tech.tamaki.alcoholmeter.ui.screens.unsupported

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

@Composable
fun UnsupportedScreen(state: UnsupportedUiState) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_triangle_exclamation_point),
            contentDescription = null,
            modifier = Modifier.padding(bottom = 20.dp).height(72.dp).fillMaxWidth(0.3f).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)).padding(16.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
        )
        Text(stringResource(R.string.unsupported_title), fontSize = 32.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(state.explanation, Modifier.padding(top = 14.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        HorizontalDivider(Modifier.padding(vertical = 20.dp), color = MaterialTheme.colorScheme.outline)
        Text(stringResource(R.string.unsupported_versions), fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(80.dp))
    }
}

private val previewState = UnsupportedUiState("このアプリは Health Connect の飲酒記録に対応した端末で利用できます。")

@Preview(name = "Unsupported Light", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun UnsupportedLightPreview() { AlcoholMeterTheme(false) { Surface { UnsupportedScreen(previewState) } } }

@Preview(name = "Unsupported Dark", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun UnsupportedDarkPreview() { AlcoholMeterTheme(true) { Surface { UnsupportedScreen(previewState) } } }

@Preview(name = "Unsupported 200%", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 2f)
@Composable
private fun UnsupportedLargeTextPreview() { AlcoholMeterTheme(true) { Surface { UnsupportedScreen(previewState) } } }
