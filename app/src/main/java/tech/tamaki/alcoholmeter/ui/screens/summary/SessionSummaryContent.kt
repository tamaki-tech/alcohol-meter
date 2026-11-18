package tech.tamaki.alcoholmeter.ui.screens.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import tech.tamaki.alcoholmeter.ui.components.AppButton
import tech.tamaki.alcoholmeter.ui.components.AppButtonStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.tamaki.alcoholmeter.R
import tech.tamaki.alcoholmeter.ui.components.DrinkColorDot

@Composable
fun SessionSummaryContent(state: SummaryUiState, onEditDrink: ((String) -> Unit)? = null) {
    Column(Modifier.fillMaxWidth()) {
        MetricGrid(state.metrics)
        DrinkCategoryTags(state.categories)
        BacTrendCard(state)
        Text(stringResource(R.string.summary_drink_order), Modifier.padding(top = 22.dp, bottom = 4.dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        // 履歴詳細では同じ行から編集できるようにする（閲覧用と編集用の 2 つのリストを並べない）
        state.drinkRows.forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(if (onEditDrink != null) 52.dp else 48.dp)
                    .then(if (onEditDrink != null) Modifier.clickable { onEditDrink(row.id) } else Modifier)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.time, Modifier.padding(end = 12.dp), fontSize = 14.sp)
                DrinkColorDot(row.colorIndex)
                Text(row.name, Modifier.weight(1f).padding(start = 10.dp), fontSize = 14.sp, maxLines = 1)
                Text(row.alcohol, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                if (onEditDrink != null) {
                    AppButton("", { onEditDrink(row.id) }, Modifier.size(48.dp), AppButtonStyle.Ghost, 48.dp, icon = R.drawable.ic_pencil, iconContentDescription = stringResource(R.string.history_edit_drink_named, row.name))
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(metrics: List<SessionMetricUiState>) {
    val dividerColor = MaterialTheme.colorScheme.outline
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        metrics.take(4).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(76.dp)) {
                row.forEachIndexed { column, metric ->
                    Column(
                        Modifier.weight(1f)
                            .then(if (column == 1) Modifier.drawBehind { drawLine(dividerColor, Offset.Zero, Offset(0f, size.height), 1.dp.toPx()) } else Modifier)
                            .padding(start = if (column == 1) 14.dp else 0.dp),
                    ) {
                        Text(metric.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(metric.value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(dividerColor, Offset.Zero, Offset(size.width, 0f))
            }
        }
    }
}

@Composable
private fun DrinkCategoryTags(categories: List<DrinkCategoryUiState>) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.forEach { category ->
            Row(Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                DrinkColorDot(category.colorIndex, size = 8.dp)
                Text("${category.label} ${category.count}", Modifier.padding(start = 6.dp), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun BacTrendCard(state: SummaryUiState) {
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline
    Column(Modifier.fillMaxWidth().padding(top = 20.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text("推定 BAC の推移", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("最大 ${state.maximumBac}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Canvas(Modifier.fillMaxWidth().height(142.dp).padding(top = 10.dp)) {
            if (state.bacPoints.isNotEmpty()) {
                val maxBac = state.bacPoints.maxOf { it.bac }.coerceAtLeast(0.001f)
                val path = Path()
                state.bacPoints.forEachIndexed { index, point ->
                    val x = size.width * index / state.bacPoints.lastIndex.coerceAtLeast(1)
                    val y = size.height * (1f - point.bac / maxBac)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                val fill = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(fill, primary.copy(alpha = 0.16f))
                drawPath(path, primary, style = Stroke(width = 1.75.dp.toPx()))
                state.bacPoints.forEachIndexed { index, point ->
                    point.colorIndex?.let { colorIndex ->
                        val x = size.width * index / state.bacPoints.lastIndex.coerceAtLeast(1)
                        val y = size.height * (1f - point.bac / maxBac)
                        drawCircle(presetColor(colorIndex), 3.dp.toPx(), Offset(x, y))
                    }
                }
                drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.bacPoints.filterIndexed { index, _ -> index % 24 == 0 }.forEach { point ->
                Text(point.timeLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Text("しらふの目安 ${state.soberEta} ごろ", Modifier.padding(top = 12.dp), fontSize = 14.sp)
        Text(androidx.compose.ui.res.stringResource(R.string.bac_caution), Modifier.padding(top = 4.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

private fun presetColor(index: Int) = tech.tamaki.alcoholmeter.ui.theme.DrinkColorsDark[index.coerceIn(0, 5)]
