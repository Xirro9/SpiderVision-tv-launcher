package com.spidervision.tv.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import kotlin.math.min

/*
 * The 18 custom brand marks from the prototype, each drawn in a 160×90 box.
 * Paths and sizes are ported 1:1 from the React source. For household use only.
 */

/** Colours for one brand: original tile vs the unified dark "custom" tile. */
class BrandStyle(
    val bg: Color, // original tile background (Instagram uses its gradient's middle stop)
    val label: Color, // label colour on the original background
    val ink: Color, // mark colour on the original background
    val customInk: Color, // mark colour on the custom dark tile
    val mark: MarkBuilder.(Color) -> Unit,
)

/** Collects draw operations in 160×90 mark space; text is measured once, at cache time. */
class MarkBuilder(private val measurer: TextMeasurer, private val density: Density) {
    val ops = mutableListOf<DrawScope.() -> Unit>()

    fun fill(d: String, c: Color) { val p = parsePath(d); ops += { drawPath(p, c) } }
    fun stroke(d: String, c: Color, w: Float, round: Boolean = false) {
        val p: Path = parsePath(d)
        val st = Stroke(w, cap = if (round) StrokeCap.Round else StrokeCap.Butt, join = if (round) StrokeJoin.Round else StrokeJoin.Miter)
        ops += { drawPath(p, c, style = st) }
    }
    fun circle(cx: Float, cy: Float, r: Float, c: Color) { ops += { drawCircle(c, r, Offset(cx, cy)) } }
    fun ring(cx: Float, cy: Float, r: Float, c: Color, w: Float) { ops += { drawCircle(c, r, Offset(cx, cy), style = Stroke(w)) } }
    fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float, c: Color, strokeW: Float? = null) {
        ops += {
            if (strokeW == null) drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r))
            else drawRoundRect(c, Offset(x, y), Size(w, h), CornerRadius(r), style = Stroke(strokeW))
        }
    }

    /** SVG-style text: [size] in mark units, [y] is the baseline, centered unless [x] is given. */
    fun text(
        t: AnnotatedString, c: Color, size: Float, y: Float? = null, x: Float? = null,
        weight: Int = 800, serif: Boolean = false, italic: Boolean = false,
    ) {
        val style = with(density) {
            TextStyle(
                color = c,
                fontSize = size.toSp(),
                fontFamily = if (serif) FontFamily.Serif else Manrope,
                fontWeight = FontWeight(weight),
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                letterSpacing = (if (serif) 0f else -1.2f).toSp(),
            )
        }
        val layout = measurer.measure(t, style, softWrap = false)
        val baseline = y ?: (45f + size * 0.36f)
        val left = x ?: (80f - layout.size.width / 2f)
        val top = baseline - layout.firstBaseline
        ops += { drawText(layout, topLeft = Offset(left, top)) }
    }

    fun text(t: String, c: Color, size: Float, y: Float? = null, x: Float? = null, weight: Int = 800, serif: Boolean = false, italic: Boolean = false) =
        text(AnnotatedString(t), c, size, y, x, weight, serif, italic)
}

private val W = Color.White
private fun hex(v: Long) = Color(v)

val BRAND_STYLES: Map<String, BrandStyle> = mapOf(
    "netflix" to BrandStyle(hex(0xFF000000), hex(0xFFF2F3F5), hex(0xFFE50914), hex(0xFFE50914)) { ink ->
        fill("M64 16h13l19 38V16h13v58H96L77 36v38H64Z", ink)
    },
    "youtube" to BrandStyle(hex(0xFFFFFFFF), hex(0xFF0F0F0F), hex(0xFFFF0000), hex(0xFFFF0000)) { ink ->
        roundRect(42f, 20f, 76f, 52f, 14f, ink); fill("M72 33v26l22-13Z", W)
    },
    "prime" to BrandStyle(hex(0xFF0F171E), hex(0xFFF2F3F5), hex(0xFF1A98FF), hex(0xFF1A98FF)) { ink ->
        text("prime", ink, 34f, y = 44f)
        stroke("M50 52q30 13 60 0", ink, 4f, round = true)
        stroke("M104 47l7 5-8 3", ink, 3.5f, round = true)
        text("video", ink, 20f, y = 76f, weight = 700)
    },
    "disney" to BrandStyle(hex(0xFF0B1A55), hex(0xFFF2F3F5), W, hex(0xFFE6ECFA)) { ink ->
        stroke("M30 62q50-46 100-10", ink, 2f)
        text("Disney+", ink, 36f, y = 60f, weight = 600, serif = true, italic = true)
    },
    "max" to BrandStyle(hex(0xFF002BE7), W, W, hex(0xFF6C8BFF)) { ink -> text("max", ink, 50f) },
    "crunchyroll" to BrandStyle(hex(0xFFF47521), hex(0xFF1A0E05), W, hex(0xFFF47521)) { ink ->
        ring(80f, 45f, 27f, ink, 8f); circle(90f, 38f, 12f, ink)
    },
    "spotify" to BrandStyle(hex(0xFF121212), hex(0xFFF2F3F5), hex(0xFF1DB954), hex(0xFF1DB954)) { ink ->
        circle(80f, 45f, 30f, ink)
        val bg = hex(0xFF121212)
        stroke("M62 36q19-7 37 3", bg, 5.5f, round = true)
        stroke("M64 46q15-5 31 2", bg, 4.5f, round = true)
        stroke("M66 55q12-4 25 2", bg, 3.8f, round = true)
    },
    "ytmusic" to BrandStyle(hex(0xFF0F0F0F), hex(0xFFF2F3F5), hex(0xFFFF0000), hex(0xFFFF0000)) { ink ->
        circle(80f, 45f, 30f, ink); ring(80f, 45f, 16f, W, 3f); fill("M75 37v16l13-8Z", W)
    },
    "hulu" to BrandStyle(hex(0xFF0B0C0F), hex(0xFFF2F3F5), hex(0xFF1CE783), hex(0xFF1CE783)) { ink -> text("hulu", ink, 50f) },
    "appletv" to BrandStyle(hex(0xFF000000), hex(0xFFF2F3F5), W, hex(0xFFE8E9ED)) { ink ->
        text("apple", ink, 22f, y = 38f, weight = 600); text("tv", ink, 44f, y = 74f)
    },
    "peacock" to BrandStyle(hex(0xFF000000), hex(0xFFF2F3F5), W, W) { ink ->
        val dots = listOf(0xFFFFC700, 0xFFFF7112, 0xFFEF1541, 0xFF6E55DC, 0xFF069DE0, 0xFF05AC3F)
        val ys = listOf(38f, 30f, 26f, 26f, 30f, 38f)
        dots.forEachIndexed { i, c -> circle(55f + i * 10, ys[i], 4.6f, hex(c)) }
        text("peacock", ink, 26f, y = 70f)
    },
    "googletv" to BrandStyle(hex(0xFF202124), hex(0xFFF2F3F5), hex(0xFFE8EAED), hex(0xFFE8EAED)) { ink ->
        val g = buildAnnotatedString {
            listOf('G' to 0xFF4285F4, 'o' to 0xFFEA4335, 'o' to 0xFFFBBC05, 'g' to 0xFF4285F4, 'l' to 0xFF34A853, 'e' to 0xFFEA4335)
                .forEach { (ch, c) -> withStyle(SpanStyle(color = hex(c))) { append(ch) } }
            withStyle(SpanStyle(color = ink)) { append(" TV") }
        }
        text(g, ink, 30f, y = 57f, weight = 700)
    },
    "tubi" to BrandStyle(hex(0xFF7408FF), W, hex(0xFFFFFF13), hex(0xFFFFFF13)) { ink -> text("tubi", ink, 52f) },
    "pluto" to BrandStyle(hex(0xFF000000), hex(0xFFF2F3F5), W, W) { ink ->
        circle(42f, 45f, 15f, hex(0xFFFFF200)); stroke("M22 50q20-14 40-10", hex(0xFF000000), 3f)
        text("pluto", ink, 34f, y = 48f, x = 64f); text("tv", ink, 20f, y = 70f, x = 66f, weight = 700)
    },
    "roku" to BrandStyle(hex(0xFF6C3C97), W, W, hex(0xFFB48BE6)) { ink ->
        text("The Roku", ink, 28f, y = 42f); text("Channel", ink, 28f, y = 72f, weight = 600)
    },
    "instagram" to BrandStyle(hex(0xFFDD2A7B), W, W, hex(0xFFF06292)) { ink ->
        roundRect(56f, 21f, 48f, 48f, 14f, ink, strokeW = 6f); ring(80f, 45f, 11f, ink, 6f); circle(95f, 30f, 3.6f, ink)
    },
    "applemusic" to BrandStyle(hex(0xFFFA243C), W, W, hex(0xFFFF5A6B)) { ink ->
        fill("M95 18v38a10 10 0 1 1-6-9.2V31l-24 6v28a10 10 0 1 1-6-9.2V27Z", ink)
    },
    "aircast" to BrandStyle(hex(0xFF1565C0), W, W, hex(0xFF5AA6F5)) { ink ->
        stroke("M52 32v-6a4 4 0 0 1 4-4h50a4 4 0 0 1 4 4v40a4 4 0 0 1-4 4H84", ink, 5f, round = true)
        stroke("M52 46a22 22 0 0 1 22 22M52 56a12 12 0 0 1 12 12", ink, 5f, round = true)
        circle(54f, 67f, 4f, ink)
    },
)

/** Draws a brand mark scaled to fit its bounds, keeping the 16:9 mark box centered. */
@Composable
fun BrandMark(key: String, custom: Boolean, modifier: Modifier) {
    val style = BRAND_STYLES[key] ?: return
    val measurer = rememberTextMeasurer(cacheSize = 0)
    Spacer(
        modifier.drawWithCache {
            val ops = MarkBuilder(measurer, this).apply { style.mark(this, if (custom) style.customInk else style.ink) }.ops
            val s = min(size.width / 160f, size.height / 90f)
            val dx = (size.width - 160f * s) / 2f
            val dy = (size.height - 90f * s) / 2f
            onDrawBehind {
                translate(dx, dy) { scale(s, s, pivot = Offset.Zero) { ops.forEach { it() } } }
            }
        },
    )
}
