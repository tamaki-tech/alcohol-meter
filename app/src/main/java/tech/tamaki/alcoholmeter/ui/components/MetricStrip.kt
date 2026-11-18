package tech.tamaki.alcoholmeter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.theme.AlcoholMeterTheme

data class MetricValue(val label: String, val value: String, val unit: String = "", val highlighted: Boolean = false)

@Composable
fun MetricStrip(
    elapsed: MetricValue,
    cups: MetricValue,
    bac: MetricValue,
    modifier: Modifier = Modifier,
    cupCount: Int,
) {
    var previousCups by remember { mutableIntStateOf(cupCount) }
    var pulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(cupCount) {
        if (cupCount != previousCups) {
            previousCups = cupCount
            pulse = 1
            delay(180)
            pulse = 0
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (pulse % 2 == 1) 1.25f else 1f,
        animationSpec = tween(durationMillis = if (pulse == 1) 90 else 90),
        label = "cup-count-pulse",
    )
    Row(
        modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        MetricCell(elapsed, Modifier.weight(1.25f))
        MetricDivider()
            MetricCell(cups, Modifier.weight(1f).padding(start = 12.dp), Modifier.graphicsLayer(scaleX = scale, scaleY = scale, transformOrigin = TransformOrigin(0f, 1f)))
        MetricDivider()
        MetricCell(bac, Modifier.weight(1.15f).padding(start = 12.dp))
    }
}

@Composable
private fun MetricCell(metric: MetricValue, modifier: Modifier = Modifier, valueModifier: Modifier = Modifier) {
    Column(modifier.padding(end = 6.dp)) {
        Text(metric.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        FittedMetricValue(metric, valueModifier)
    }
}

/**
 * 指標の値を、セルの幅に収まる最大の文字サイズ（上限 34sp）で 1 行に表示する。
 * 文字数や端末の文字サイズ設定（200% など）で数字が途中で切れないようにする。
 * 等幅フォントなので、幅は文字数だけで決まる（毎秒変わる経過時間でも測り直しは文字数が変わったときだけ）
 */
@Composable
private fun FittedMetricValue(metric: MetricValue, valueModifier: Modifier) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val maxWidthPx = constraints.maxWidth
        val unitStyle = TextStyle(fontSize = 15.sp)
        val valueSize = remember(metric.value.length, metric.unit, maxWidthPx, density) {
            val unitWidth = if (metric.unit.isEmpty()) 0 else measurer.measure(metric.unit, unitStyle).size.width + with(density) { 2.dp.roundToPx() }
            var size = 34f
            while (size > 12f && measurer.measure(metric.value, TextStyle(fontSize = size.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)).size.width + unitWidth > maxWidthPx) size -= 1f
            size
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = valueModifier) {
            Text(
                metric.value,
                fontSize = valueSize.sp,
                lineHeight = (valueSize * 1.12f).sp,
                fontWeight = FontWeight.SemiBold,
                color = if (metric.highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                fontFamily = FontFamily.Monospace,
            )
            if (metric.unit.isNotEmpty()) {
                Text(metric.unit, style = unitStyle, maxLines = 1, modifier = Modifier.padding(start = 2.dp, bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun MetricDivider() {
    val dividerColor = MaterialTheme.colorScheme.outline
    Box(Modifier.width(1.dp).height(52.dp)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(52.dp)) {
            drawLine(dividerColor, start = androidx.compose.ui.geometry.Offset(size.width / 2, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 2, size.height))
        }
    }
}

@Preview(name = "MetricStrip Light", showBackground = true)
@Composable
private fun MetricStripLightPreview() {
    AlcoholMeterTheme(false) { MetricStrip(MetricValue("経過", "1:42"), MetricValue("杯数", "4", "杯"), MetricValue("推定 BAC", "0.045", "%", true), cupCount = 4) }
}

@Preview(name = "MetricStrip Dark", showBackground = true)
@Composable
private fun MetricStripDarkPreview() {
    AlcoholMeterTheme(true) { MetricStrip(MetricValue("経過", "1:42"), MetricValue("杯数", "4", "杯"), MetricValue("推定 BAC", "0.045", "%", true), cupCount = 4) }
}

@Preview(name = "MetricStrip 200%", showBackground = true, fontScale = 2f)
@Composable
private fun MetricStripLargeTextPreview() {
    AlcoholMeterTheme(true) { MetricStrip(MetricValue("経過", "1:42"), MetricValue("杯数", "4", "杯"), MetricValue("推定 BAC", "0.045", "%", true), cupCount = 4) }
}
