package com.spidervision.tv.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/* Everything in this file is static artwork: drawn once, never animated. */

fun parsePath(d: String): Path = PathParser().parsePathString(d).toPath()

/** Spokes plus sagging threads, ported from the prototype's `webPaths`. */
private fun webPath(cx: Float, cy: Float, startDeg: Float, endDeg: Float, spokes: Int, radii: List<Float>): Path {
    val angles = List(spokes) { Math.toRadians((startDeg + (endDeg - startDeg) * it / (spokes - 1)).toDouble()) }
    fun pt(a: Double, r: Float) = Offset(cx + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat())
    val p = Path()
    val maxR = radii.last()
    for (a in angles) {
        val e = pt(a, maxR * 1.08f)
        p.moveTo(cx, cy); p.lineTo(e.x, e.y)
    }
    for (r in radii) for (i in 0 until angles.size - 1) {
        val a1 = pt(angles[i], r)
        val a2 = pt(angles[i + 1], r)
        val m = pt((angles[i] + angles[i + 1]) / 2, r * 0.86f)
        p.moveTo(a1.x, a1.y); p.quadraticTo(m.x, m.y, a2.x, a2.y)
    }
    return p
}

private val CORNER_WEB = webPath(100f, 0f, 90f, 180f, 5, listOf(26f, 50f, 74f, 98f))
private val FULL_WEB = webPath(50f, 50f, 0f, 360f, 9, listOf(14f, 28f, 42f))

/** Web in the top-right corner of a tile; [Modifier] decides its square size. */
@Composable
fun WebCorner(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        scale(s, s, pivot = Offset.Zero) { drawPath(CORNER_WEB, color, style = Stroke(width = 1.2f / s)) }
    }
}

@Composable
fun WebGlyph(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 100f
        scale(s, s, pivot = Offset.Zero) {
            drawCircle(C.suit, 49f, Offset(50f, 50f))
            drawPath(FULL_WEB, C.silver, style = Stroke(width = 2.2f))
            drawCircle(C.red, 5f, Offset(50f, 50f))
        }
    }
}

/**
 * Halftone dots fading away from one corner. Dots are grouped into a few alpha bands and drawn
 * with one point call per band, cached until the size changes.
 */
fun Modifier.halftone(color: Color, gap: Dp, radius: Dp, fromBottomRight: Boolean = true) = drawWithCache {
    val g = gap.toPx()
    val r = radius.toPx()
    val bands = 6
    val points = List(bands) { mutableListOf<Offset>() }
    // Fade direction from the prototype's mask gradient: strongest at the bottom corner.
    val start = Offset(if (fromBottomRight) size.width else 0f, size.height)
    val end = Offset(if (fromBottomRight) size.width * 0.15f else size.width * 0.85f, size.height * 0.1f)
    val dir = end - start
    val len2 = dir.x * dir.x + dir.y * dir.y
    var y = g / 2
    while (y < size.height) {
        var x = g / 2
        while (x < size.width) {
            val t = (((x - start.x) * dir.x + (y - start.y) * dir.y) / len2).coerceIn(0f, 1f)
            val alpha = 1f - t
            val band = (alpha * bands).toInt().coerceAtMost(bands - 1)
            if (alpha > 0.05f) points[band] += Offset(x, y)
            x += g
        }
        y += g
    }
    onDrawBehind {
        points.forEachIndexed { i, pts ->
            if (pts.isNotEmpty()) drawPoints(pts, PointMode.Points, color.copy(alpha = (i + 1f) / bands), strokeWidth = r * 2, cap = StrokeCap.Round)
        }
    }
}

private val BURST_POINTS: List<Offset> = List(24) {
    val a = it / 24.0 * Math.PI * 2
    val r = if (it % 2 == 1) 37f else 50f
    Offset((50 + cos(a) * r).toFloat(), (50 + sin(a) * r).toFloat())
}

/** Red comic starburst with cream lettering. */
@Composable
fun Burst(text: String, size: Dp, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = this.size.minDimension / 100f
            val p = Path().apply {
                BURST_POINTS.forEachIndexed { i, o -> if (i == 0) moveTo(o.x * s, o.y * s) else lineTo(o.x * s, o.y * s) }
                close()
            }
            drawPath(p, C.red)
            drawPath(p, C.cream, style = Stroke(width = 1.5f * s * 2))
        }
        BasicText(
            text,
            modifier = Modifier.rotate(-6f).padding(horizontal = size * 0.16f),
            style = TextStyle(fontFamily = Bangers, fontSize = fontSize, color = C.cream, letterSpacing = 0.04.em, textAlign = TextAlign.Center),
        )
    }
}

/** Cream comic caption box, skewed −6° like the prototype's `-skew-x-6`. */
@Composable
fun Caption(text: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    BasicText(
        text.uppercase(),
        modifier = modifier
            .drawBehind {
                val dx = size.height / 2 * 0.105f // tan(6°)
                val p = Path().apply {
                    moveTo(dx, 0f); lineTo(size.width + dx, 0f); lineTo(size.width - dx, size.height); lineTo(-dx, size.height); close()
                }
                drawPath(p, C.cream)
                drawPath(p, C.ink, style = Stroke(width = 1.dp.toPx()))
            }
            .padding(horizontal = 5.dp, vertical = 1.dp),
        style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = fontSize, color = C.ink, letterSpacing = 0.08.em),
        maxLines = 1,
    )
}

/**
 * The single yellow focus outline, drawn outside the control's bounds. Must be applied
 * before any clip so it isn't cut off.
 */
fun Modifier.focusRing(focused: Boolean, corner: Dp = 2.dp, width: Dp = 3.dp, offset: Dp = 2.dp) = drawWithContent {
    drawContent()
    if (focused) {
        val w = width.toPx()
        val o = offset.toPx() + w / 2
        drawRoundRect(
            C.focus,
            topLeft = Offset(-o, -o),
            size = Size(size.width + 2 * o, size.height + 2 * o),
            cornerRadius = CornerRadius(corner.toPx() + o),
            style = Stroke(width = w),
        )
    }
}

/** Small stroke icon from 24×24 SVG path data. */
@Composable
fun StrokeIcon(paths: List<Path>, color: Color, size: Dp, strokeWidth: Float = 1.7f, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = min(this.size.width, this.size.height) / 24f
        translate(0f, 0f) {
            scale(s, s, pivot = Offset.Zero) {
                paths.forEach { drawPath(it, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round)) }
            }
        }
    }
}

object Icons {
    val gear = listOf(
        parsePath("M15.2 12a3.2 3.2 0 1 1-6.4 0a3.2 3.2 0 1 1 6.4 0Z"),
        parsePath("M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z"),
    )
    val tv = listOf(parsePath("M4.5 5h15a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 15.5v-9A1.5 1.5 0 0 1 4.5 5Z"), parsePath("M8 21h8M12 17v4"))
    val grid = listOf(
        parsePath("M4.5 3.5h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z"),
        parsePath("M14.5 3.5h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z"),
        parsePath("M4.5 13.5h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z"),
        parsePath("M14.5 13.5h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1h-5a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1Z"),
    )
    val shield = listOf(parsePath("M12 3 4.5 6v5.5c0 4.6 3.2 8.3 7.5 9.5 4.3-1.2 7.5-4.9 7.5-9.5V6Z"), parsePath("m8.8 12.2 2.3 2.3 4.2-4.6"))
}
