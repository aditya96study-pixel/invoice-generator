package com.amitabhtours.invoice.ui.earnings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

data class BarGroup(val label: String, val values: List<Float>, val tooltipTitle: String = label)

/**
 * Stacked bar chart. Thin bars with rounded tops, 2 dp surface gaps, recessive grid,
 * x labels spaced so they never touch. Tap a bar to see its values.
 */
@Composable
fun BarChart(
    groups: List<BarGroup>,
    colors: List<Color>,
    seriesNames: List<String>,
    format: (Float) -> String,
    modifier: Modifier = Modifier,
    height: Int = 200,
    minScale: Float = 1f,
) {
    val tm = rememberTextMeasurer()
    var selected by remember(groups) { mutableStateOf<Int?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val ink = MaterialTheme.colorScheme.onSurface
    val labelStyle = TextStyle(fontSize = 10.sp, color = muted)
    val niceMax = niceCeil(max(minScale, groups.maxOfOrNull { it.values.sum() } ?: 0f))
    val ticks = yTicks(niceMax, format)
    val left = axisWidth(tm, ticks, labelStyle)

    Column(modifier) {
        if (seriesNames.size > 1) Legend(seriesNames, colors)
        Box(Modifier.fillMaxWidth().height(height.dp)) {
            Canvas(
                Modifier.fillMaxWidth().height(height.dp).pointerInput(groups, left) {
                    detectTapGestures { pos ->
                        val w = size.width - left
                        if (groups.isEmpty() || pos.x < left) { selected = null; return@detectTapGestures }
                        val slot = w / groups.size
                        val i = ((pos.x - left) / slot).toInt().coerceIn(0, groups.size - 1)
                        selected = if (selected == i) null else i
                    }
                },
            ) {
                val bottom = size.height - 18.dp.toPx()
                val top = 8.dp.toPx()
                val plotH = bottom - top
                drawYAxis(tm, ticks, niceMax, left, top, bottom, grid, labelStyle)
                if (groups.isEmpty()) return@Canvas
                val slot = (size.width - left) / groups.size
                val gap = 2.dp.toPx()
                val barW = (slot - gap).coerceAtMost(28.dp.toPx()).coerceAtLeast(2f)
                groups.forEachIndexed { i, g ->
                    val x = left + slot * i + (slot - barW) / 2f
                    var y = bottom
                    val topIndex = g.values.indices.lastOrNull { g.values[it] > 0f }
                    g.values.forEachIndexed { s, v ->
                        if (v <= 0f) return@forEachIndexed
                        val h = plotH * (v / niceMax)
                        val segTop = y - h
                        val color = colors[s % colors.size].copy(alpha = if (selected == null || selected == i) 1f else 0.45f)
                        if (s == topIndex) {
                            val r = 4.dp.toPx().coerceAtMost(barW / 2)
                            val path = Path().apply {
                                addRoundRect(RoundRect(Rect(x, segTop, x + barW, y), topLeft = CornerRadius(r, r), topRight = CornerRadius(r, r)))
                            }
                            drawPath(path, color)
                        } else {
                            drawRect(color, Offset(x, segTop), Size(barW, h))
                            // 2dp surface gap between stacked segments
                            drawLine(surface, Offset(x, segTop), Offset(x + barW, segTop), strokeWidth = gap)
                        }
                        y = segTop
                    }
                }
                drawXLabels(tm, groups.map { it.label }, { i -> left + slot * i + slot / 2f }, slot, left, bottom + 3.dp.toPx(), labelStyle)
                drawLine(muted.copy(alpha = 0.6f), Offset(left, bottom), Offset(size.width, bottom), strokeWidth = 1.5f)
            }
            selected?.let { i ->
                val g = groups.getOrNull(i) ?: return@let
                Tooltip(
                    title = g.tooltipTitle,
                    rows = seriesNames.mapIndexed { s, n -> Triple(n, format(g.values.getOrElse(s) { 0f }), colors[s % colors.size]) } +
                        if (seriesNames.size > 1) listOf(Triple("Total", format(g.values.sum()), Color.Transparent)) else emptyList(),
                    modifier = tooltipPlacement(i, groups.size, left),
                )
            }
        }
    }
}

/** Single-series line chart (2 dp line, marker on tap). */
@Composable
fun LineChart(
    points: List<Pair<String, Float>>,
    color: Color,
    format: (Float) -> String,
    title: (Int) -> String,
    modifier: Modifier = Modifier,
    height: Int = 160,
    seriesName: String = "Trips",
    wholeNumbers: Boolean = true,
) {
    val tm = rememberTextMeasurer()
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val labelStyle = TextStyle(fontSize = 10.sp, color = muted)
    val raw = niceCeil(max(if (wholeNumbers) 2f else 1f, points.maxOfOrNull { it.second } ?: 0f))
    // Counts: keep the middle grid line on a whole number (0, 3, 6 rather than 0, 2.5, 5).
    val niceMax = if (wholeNumbers) ceil(raw).let { if (it.toInt() % 2 == 1) it + 1 else it } else raw
    val ticks = yTicks(niceMax, format)
    val left = axisWidth(tm, ticks, labelStyle)
    val rightPad = with(LocalDensity.current) { 12.dp.toPx() }
    Box(modifier.fillMaxWidth().height(height.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(height.dp).pointerInput(points, left) {
                detectTapGestures { pos ->
                    if (points.size < 2) return@detectTapGestures
                    val step = (size.width - left - rightPad) / (points.size - 1)
                    val i = ((pos.x - left) / step + 0.5f).toInt().coerceIn(0, points.size - 1)
                    selected = if (selected == i) null else i
                }
            },
        ) {
            val right = size.width - rightPad
            val bottom = size.height - 18.dp.toPx()
            val top = 8.dp.toPx()
            val plotH = bottom - top
            drawYAxis(tm, ticks, niceMax, left, top, bottom, grid, labelStyle)
            if (points.size < 2) return@Canvas
            val step = (right - left) / (points.size - 1)
            val path = Path()
            points.forEachIndexed { i, (_, v) ->
                val x = left + step * i
                val y = bottom - plotH * (v / niceMax)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            drawXLabels(tm, points.map { it.first }, { i -> left + step * i }, step, left, bottom + 3.dp.toPx(), labelStyle)
            selected?.let { i ->
                val x = left + step * i
                val y = bottom - plotH * (points[i].second / niceMax)
                drawLine(muted.copy(alpha = 0.5f), Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
                drawCircle(surface, 6.dp.toPx(), Offset(x, y))
                drawCircle(color, 4.dp.toPx(), Offset(x, y))
            }
        }
        selected?.let { i ->
            val p = points.getOrNull(i) ?: return@let
            Tooltip(title(i), listOf(Triple(seriesName, format(p.second), color)), tooltipPlacement(i, points.size, left))
        }
    }
}

/** Donut with 2 dp surface gaps; the legend with values doubles as the table view. */
@Composable
fun Donut(slices: List<Pair<String, Float>>, colors: List<Color>, format: (Float) -> String, centerLabel: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Side by side only when the legend has room for name and amount on one line.
        if (maxWidth >= 400.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DonutRing(slices, colors, format, centerLabel)
                Spacer(Modifier.width(16.dp))
                DonutLegend(slices, colors, format, Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                DonutRing(slices, colors, format, centerLabel)
                Spacer(Modifier.height(14.dp))
                DonutLegend(slices, colors, format, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DonutRing(slices: List<Pair<String, Float>>, colors: List<Color>, format: (Float) -> String, centerLabel: String) {
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val total = slices.sumOf { it.second.toDouble() }.toFloat()
    var selected by remember(slices) { mutableStateOf<Int?>(null) }
    val tm = rememberTextMeasurer()
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        Modifier.size(160.dp).pointerInput(slices) {
            detectTapGestures { pos ->
                val c = Offset(size.width / 2f, size.height / 2f)
                val d = pos - c
                val r = sqrt(d.x * d.x + d.y * d.y)
                if (r < size.width * 0.25f || total <= 0f) { selected = null; return@detectTapGestures }
                var ang = Math.toDegrees(atan2(d.y.toDouble(), d.x.toDouble())).toFloat() + 90f
                if (ang < 0) ang += 360f
                var acc = 0f
                slices.forEachIndexed { i, (_, v) ->
                    val sweep = v / total * 360f
                    if (ang >= acc && ang < acc + sweep) { selected = if (selected == i) null else i; return@detectTapGestures }
                    acc += sweep
                }
            }
        },
    ) {
        val stroke = size.width * 0.16f
        val inset = stroke / 2
        var start = -90f
        if (total <= 0f) {
            drawArc(muted.copy(alpha = 0.2f), 0f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
        }
        slices.forEachIndexed { i, (_, v) ->
            if (v <= 0f) return@forEachIndexed
            val sweep = v / total * 360f
            val c = colors[i % colors.size].copy(alpha = if (selected == null || selected == i) 1f else 0.4f)
            drawArc(c, start, sweep, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke))
            if (slices.count { it.second > 0f } > 1) {
                val rad = Math.toRadians(start.toDouble())
                val cx = size.width / 2
                val r1 = size.width / 2 - stroke
                val r2 = size.width / 2
                drawLine(
                    surface,
                    Offset(cx + (r1 * kotlin.math.cos(rad)).toFloat(), cx + (r1 * kotlin.math.sin(rad)).toFloat()),
                    Offset(cx + (r2 * kotlin.math.cos(rad)).toFloat(), cx + (r2 * kotlin.math.sin(rad)).toFloat()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            start += sweep
        }
        // Centre text shrinks to fit inside the hole.
        val label = selected?.let { format(slices[it].second) } ?: centerLabel
        val hole = size.width - 2 * stroke - 12.dp.toPx()
        var fs = 15f
        var t = tm.measure(label, TextStyle(fontSize = fs.sp, fontWeight = FontWeight.Bold, color = ink))
        while (t.size.width > hole && fs > 9f) {
            fs -= 1f
            t = tm.measure(label, TextStyle(fontSize = fs.sp, fontWeight = FontWeight.Bold, color = ink))
        }
        drawText(t, topLeft = Offset(size.width / 2 - t.size.width / 2f, size.height / 2 - t.size.height / 2f))
    }
}

@Composable
private fun DonutLegend(slices: List<Pair<String, Float>>, colors: List<Color>, format: (Float) -> String, modifier: Modifier) {
    val total = slices.sumOf { it.second.toDouble() }.toFloat()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        slices.forEachIndexed { i, (name, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(colors[i % colors.size]))
                Spacer(Modifier.width(8.dp))
                Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(format(v), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, softWrap = false)
                Text(
                    if (total > 0) "${Math.round(v / total * 100)}%" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, softWrap = false, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.width(40.dp),
                )
            }
        }
    }
}

@Composable
private fun Legend(names: List<String>, colors: List<Color>) {
    FlowRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        names.forEachIndexed { i, n ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(colors[i % colors.size]))
                Spacer(Modifier.width(6.dp))
                Text(n, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Tooltip(title: String, rows: List<Triple<String, String, Color>>, modifier: Modifier) {
    Column(
        modifier.padding(4.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.inverseSurface).padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.Bold)
        rows.forEach { (n, v, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (c != Color.Transparent) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c))
                    Spacer(Modifier.width(6.dp))
                }
                Text("$n: $v", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }
    }
}

/** Puts the tooltip on the side away from the tapped mark so it never hides it. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.tooltipPlacement(index: Int, count: Int, leftPx: Float): Modifier {
    val leftDp = with(LocalDensity.current) { leftPx.toDp() }
    return if (index >= count / 2) Modifier.align(Alignment.TopStart).padding(start = leftDp)
    else Modifier.align(Alignment.TopEnd)
}

private fun yTicks(niceMax: Float, format: (Float) -> String): List<Pair<Float, String>> =
    listOf(0f, niceMax / 2f, niceMax).map { it to format(it) }

/** Width reserved for the y-axis: the widest tick label plus a gap, so amounts are never cut off. */
@Composable
private fun axisWidth(tm: TextMeasurer, ticks: List<Pair<Float, String>>, style: TextStyle): Float {
    val gap = with(LocalDensity.current) { 8.dp.toPx() }
    return remember(ticks) { ticks.maxOf { tm.measure(it.second, style).size.width }.toFloat() } + gap
}

private fun DrawScope.drawYAxis(
    tm: TextMeasurer, ticks: List<Pair<Float, String>>, niceMax: Float,
    left: Float, top: Float, bottom: Float, grid: Color, style: TextStyle,
) {
    val plotH = bottom - top
    ticks.forEach { (v, label) ->
        val y = bottom - plotH * (v / niceMax)
        drawLine(grid, Offset(left, y), Offset(size.width, y), strokeWidth = 1f)
        val t = tm.measure(label, style)
        // The zero label sits just above the baseline so it can't touch the x labels below.
        val ty = if (v == 0f) y - t.size.height else y - t.size.height / 2f
        drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), ty.coerceAtLeast(0f)))
    }
}

/**
 * Draws x labels at an even spacing wide enough that no two touch. Counting back from the
 * newest point, so the latest date is always labelled. Labels never cross into the y-axis.
 */
private fun DrawScope.drawXLabels(
    tm: TextMeasurer, labels: List<String>, centerX: (Int) -> Float, slot: Float,
    left: Float, y: Float, style: TextStyle,
) {
    if (labels.isEmpty()) return
    val measured = labels.map { tm.measure(it, style) }
    val placed = xLabelPositions(measured.map { it.size.width.toFloat() }, labels.indices.map(centerX), slot, left, size.width, 8.dp.toPx())
    placed.forEach { (i, x) -> drawText(measured[i], topLeft = Offset(x, y)) }
}

/**
 * Which x labels to draw and where (index to left edge). Evenly spaced, counted back from the
 * newest so it is always shown, kept inside [left, right], and never closer than [gap].
 */
internal fun xLabelPositions(widths: List<Float>, centers: List<Float>, slot: Float, left: Float, right: Float, gap: Float): List<Pair<Int, Float>> {
    if (widths.isEmpty() || slot <= 0f) return emptyList()
    val every = max(1, ceil((widths.max() + gap) / slot).toInt())
    val out = ArrayList<Pair<Int, Float>>()
    var prevLeft = Float.MAX_VALUE
    var i = widths.lastIndex
    while (i >= 0) {
        val x = (centers[i] - widths[i] / 2f).coerceIn(left, max(left, right - widths[i]))
        if (x + widths[i] + gap <= prevLeft) {
            out += i to x
            prevLeft = x
        }
        i -= every
    }
    return out.reversed()
}

fun niceCeil(v: Float): Float {
    if (v <= 0f) return 1f
    val exp = Math.pow(10.0, Math.floor(Math.log10(v.toDouble()))).toFloat()
    val f = v / exp
    val nf = when {
        f <= 1f -> 1f
        f <= 2f -> 2f
        f <= 2.5f -> 2.5f
        f <= 5f -> 5f
        else -> 10f
    }
    return nf * exp
}
