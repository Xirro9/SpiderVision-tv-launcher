package com.spidervision.tv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.spidervision.tv.R

/*
 * Design tokens from the approved Orrery Comic Edition (see docs/DESIGN_FIDELITY.md).
 *
 * Sizing rule: the approved design was rendered at 1920×1080 CSS px and the TV lays out at
 * 960×540 dp, so 1 dp = 2 CSS px. Every dp/sp value in the UI is the React value at a
 * 1920 px viewport, halved.
 */
object C {
    val bg = Color(0xFF111216)
    val surface = Color(0xFF1A1C22)
    val suit = Color(0xFF0B0C0F)
    val ink = Color(0xFF08090B)
    val navy = Color(0xFF16203A)
    val text = Color(0xFFF2F3F5)
    val silver = Color(0xFFB8BDC8)
    val red = Color(0xFFE0353F)
    val blue = Color(0xFF4A7BE0)
    val rule = Color(0xFF262932)
    val cream = Color(0xFFF1E7CF)
    val focus = Color(0xFFFFD23F)
    val webOnTile = Color(0xFF252932)
    val halftonePage = Color(0xFF1D2028)
    val halftonePanel = Color(0xFF2A2E38)
    val disabled = Color(0xFF6B7080)
}

/** 1.0 normally; raised by the per-profile "Larger text" setting. Applied by [T]. */
val LocalTextScale = staticCompositionLocalOf { 1f }

val Bangers = FontFamily(Font(R.font.bangers_regular))

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))
