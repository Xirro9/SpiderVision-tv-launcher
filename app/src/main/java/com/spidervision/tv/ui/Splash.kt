package com.spidervision.tv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Shown once per TV boot (not again when Android merely restarts SpiderVision in the background). */
object SplashState {
    var visible by mutableStateOf(false)

    fun decide(context: android.content.Context) {
        val boot = runCatching { android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.BOOT_COUNT) }.getOrDefault(-1)
        val prefs = context.getSharedPreferences("intro", android.content.Context.MODE_PRIVATE)
        visible = boot < 0 || prefs.getInt("boot", -2) != boot
        if (visible) prefs.edit().putInt("boot", boot).apply()
    }
}

private fun ease(x: Float) = 1f - (1f - x.coerceIn(0f, 1f)).let { it * it * it }

/**
 * Spider-web intro, ~1.6 s, drawn live (no video): spokes shoot out, threads spin round, a spider
 * drops in on a line, then a comic POW starburst and the SpiderVision logo, and the whole frame
 * punches out into the board. Any key skips it.
 */
@Composable
fun SplashOverlay(onDone: () -> Unit) {
    val t = remember { Animatable(0f) }
    val logo = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }
    var finishing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { t.animateTo(1f, tween(1100, easing = LinearEasing)) }
        SoundFx.play(SoundFx.Cue.SENSE)
        delay(520)
        SoundFx.play(SoundFx.Cue.THWIP)
        launch { logo.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f)) }
        delay(820)
        finishing = true
    }
    LaunchedEffect(finishing) {
        if (finishing) {
            exit.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            onDone()
        }
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                val e = exit.value
                alpha = 1f - e
                scaleX = 1f + 0.18f * e
                scaleY = 1f + 0.18f * e
            }
            .background(C.ink),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().halftone(C.halftonePage, 6.dp, 1.4.dp, fromBottomRight = false))
        Canvas(Modifier.fillMaxSize()) { drawWeb(t.value) }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val l = logo.value
                alpha = l.coerceIn(0f, 1f)
                val s = 1.9f - 0.9f * l
                scaleX = s
                scaleY = s
                rotationZ = (1f - l) * -8f
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Burst("", 210.dp, 1.sp, Modifier.graphicsLayer { rotationZ = 8f })
                T("SpiderVision", 54.sp, color = C.cream, family = Bangers, weight = 400, tracking = 0.05.em, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(4.dp))
            T(
                "LIVING ROOM · VOL. 1", 9.sp, weight = 800, tracking = 0.16.em,
                modifier = Modifier.background(C.blue).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

/** Web that builds itself as [t] goes 0 → 1, plus a spider dropping in on its line. */
private fun DrawScope.drawWeb(t: Float) {
    val c = center
    val r = size.minDimension * 0.62f
    val spokes = 12
    val line = C.silver.copy(alpha = 0.55f)
    val stroke = 1.6.dp.toPx()

    // Spokes shoot out first.
    val sp = ease(t / 0.35f)
    val dirs = List(spokes) { i -> (i * 2 * PI / spokes + 0.13).let { Offset(cos(it).toFloat(), sin(it).toFloat()) } }
    dirs.forEach { d -> drawLine(line, c, c + d * (r * 1.15f * sp), stroke, StrokeCap.Round) }

    // Then threads spin round, one ring after another, each sagging toward the hub.
    val rings = listOf(0.18f, 0.32f, 0.47f, 0.63f, 0.8f, 0.97f)
    rings.forEachIndexed { k, frac ->
        val p = ((t - 0.18f - k * 0.06f) / 0.22f).coerceIn(0f, 1f)
        if (p <= 0f) return@forEachIndexed
        val rr = r * frac
        val n = (spokes * p).toInt()
        val path = Path()
        for (j in 0 until n) {
            val a = c + dirs[j] * rr
            val b = c + dirs[(j + 1) % spokes] * rr
            val mid = c + (dirs[j] + dirs[(j + 1) % spokes]) * (rr * 0.5f * 0.86f)
            path.moveTo(a.x, a.y)
            path.quadraticTo(mid.x, mid.y, b.x, b.y)
        }
        drawPath(path, line.copy(alpha = 0.35f + 0.35f * (1 - frac)), style = Stroke(stroke * (1.2f - frac * 0.5f)))
    }
    drawCircle(C.red, 5.dp.toPx() * (0.5f + 0.5f * sp), c)

    // A spider drops in from the top on a single thread and stops above the logo.
    val drop = ease(t / 0.5f)
    val sy = -30.dp.toPx() + (c.y - r * 0.42f + 30.dp.toPx()) * drop
    drawLine(C.cream.copy(alpha = 0.8f), Offset(c.x, 0f), Offset(c.x, sy), 1.2.dp.toPx())
    val body = 7.dp.toPx()
    for (side in listOf(-1f, 1f)) for (leg in 0 until 4) {
        val ly = sy - body * 0.4f + leg * body * 0.38f
        val kneeX = c.x + side * body * 1.5f
        val kneeY = ly - body * 0.5f + leg * body * 0.12f
        val path = Path().apply {
            moveTo(c.x + side * body * 0.4f, ly)
            lineTo(kneeX, kneeY)
            lineTo(c.x + side * body * (2.0f + leg * 0.15f), ly + body * (0.5f + leg * 0.3f))
        }
        drawPath(path, C.cream, style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round))
    }
    drawCircle(C.ink, body * 0.75f, Offset(c.x, sy + body * 0.2f))
    drawCircle(C.cream, body * 0.75f, Offset(c.x, sy + body * 0.2f), style = Stroke(1.4.dp.toPx()))
    drawCircle(C.red, body * 0.42f, Offset(c.x, sy - body * 0.75f))
    drawCircle(C.red, min(body * 0.28f, body), Offset(c.x, sy + body * 0.25f))
}
