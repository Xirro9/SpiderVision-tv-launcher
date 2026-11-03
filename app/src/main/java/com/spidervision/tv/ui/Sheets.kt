package com.spidervision.tv.ui

import android.view.Gravity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/*
 * Side sheets for app options and Settings. Each is a real dialog window, so the remote can't
 * wander onto the board behind it and Back closes it. No dim, blur or window animation.
 */

@Composable
fun SideSheet(title: String, subtitle: String, onDismiss: () -> Unit, header: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.(FocusRequester) -> Unit) {
    Dialog(onDismissRequest = { SoundFx.play(SoundFx.Cue.BACK); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(0f)
                setWindowAnimations(0)
                setGravity(Gravity.END or Gravity.CENTER_VERTICAL)
                setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.MATCH_PARENT)
            }
        }
        val first = remember { FocusRequester() }
        val slide = remember { Animatable(1f) }
        LaunchedEffect(Unit) {
            SoundFx.play(SoundFx.Cue.THWIP)
            SoundFx.hush(250)
            withFrameNanos { }
            runCatching { first.requestFocus() }
            slide.animateTo(0f, tween(200, easing = FastOutSlowInEasing))
        }
        Column(
            Modifier.width(300.dp).fillMaxHeight()
                .graphicsLayer { translationX = slide.value * 300.dp.toPx() }
                .background(C.bg).border(1.5.dp, C.cream)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Caption(title, 7.2.sp)
            Spacer(Modifier.height(10.dp))
            if (header != null) { header(); Spacer(Modifier.height(6.dp)) }
            T(subtitle, 8.6.sp, color = C.silver, maxLines = 2)
            Spacer(Modifier.height(14.dp))
            content(first)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                T("Back", 7.7.sp, weight = 600, modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(C.surface).padding(horizontal = 4.dp, vertical = 1.dp))
                T("Close", 8.6.sp, color = C.silver)
            }
        }
    }
}

/** Full-width sheet button with the yellow focus ring. */
@Composable
fun SheetButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, accent: Boolean = false, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) SoundFx.play(SoundFx.Cue.TICK) }
    T(
        text, 10.sp, weight = 700,
        color = if (!enabled) C.disabled else if (accent) C.text else C.text,
        modifier = modifier
            .graphicsLayer()
            .focusRing(focused, corner = 4.dp)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(4.dp))
            .background(if (accent) C.red else C.surface)
            .border(1.dp, if (enabled) C.rule else C.surface, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** On/off row with a visible state word, so the setting reads without colour. */
@Composable
fun SwitchRow(label: String, hint: String, on: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) SoundFx.play(SoundFx.Cue.TICK) }
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer()
            .focusRing(focused, corner = 4.dp)
            .clickable(interactionSource = source, indication = null, onClick = onToggle)
            .clip(RoundedCornerShape(4.dp))
            .background(C.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            T(label, 10.sp, weight = 700)
            Spacer(Modifier.height(2.dp))
            T(hint, 7.7.sp, color = C.silver, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        val knob by animateDpAsState(if (on) 14.dp else 0.dp, tween(160, easing = FastOutSlowInEasing), label = "knob")
        Box(
            Modifier.size(width = 30.dp, height = 16.dp).clip(CircleShape).background(if (on) C.red else C.rule).padding(2.dp),
            contentAlignment = Alignment.CenterStart,
        ) { Box(Modifier.offset(x = knob).size(12.dp).clip(CircleShape).background(C.text)) }
        Spacer(Modifier.width(6.dp))
        T(if (on) "On" else "Off", 8.6.sp, color = C.silver, weight = 600, tracking = 0.02.em, modifier = Modifier.width(18.dp))
    }
}

/** Small section label inside a sheet. */
@Composable
fun SheetLabel(text: String, color: Color = C.silver) {
    T(text.uppercase(), 7.2.sp, color = color, weight = 700, tracking = 0.1.em)
}
