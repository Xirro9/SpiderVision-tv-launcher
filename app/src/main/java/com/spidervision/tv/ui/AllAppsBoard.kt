package com.spidervision.tv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spidervision.tv.apps.BRAND_BY_PKG
import com.spidervision.tv.apps.InstalledApp

private val TILE_HEIGHT = 80.dp
private val ART_HEIGHT = 56.dp

/**
 * Every app the TV exposes, in the profile's order: favourites first as wide panels, then the
 * rest. Designed brands use their marks; every other app shows its own TV banner or icon.
 * OK opens, Menu (or holding OK) opens app options, Back closes the board (handled by the caller).
 */
@Composable
fun AllAppsBoard(
    ordered: List<InstalledApp>, favorites: List<String>, custom: Boolean, art: ArtCache, ui: UiMessages,
    onLaunch: (String) -> Unit, onMenu: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val artPx = with(LocalDensity.current) { ART_HEIGHT.roundToPx() }

    LaunchedEffect(ordered) { art.ensure(context, ordered, artPx) }

    val reqs = remember { HashMap<String, FocusRequester>() }
    fun reqFor(pkg: String) = reqs.getOrPut(pkg) { FocusRequester() }
    val lastFocus = remember { arrayOf<String?>(null) }
    // Focus the first app on open, and the last-used app when coming back from it.
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withFrameNanos { }
            val target = lastFocus[0]?.takeIf { pkg -> ordered.any { it.pkg == pkg } } ?: ordered.firstOrNull()?.pkg
            if (target != null) runCatching { reqFor(target).requestFocus() }
        }
    }

    val enter = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, androidx.compose.animation.core.tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
    Box(
        Modifier.fillMaxSize().background(C.bg).graphicsLayer {
            translationY = (1f - enter.value) * 24.dp.toPx()
        },
    ) {
        Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.5f).fillMaxHeight(0.34f).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.halftone(C.halftonePage, 5.5.dp, 1.2.dp))
        Column(Modifier.fillMaxSize().padding(start = 28.8.dp, end = 28.8.dp, top = 16.dp, bottom = 10.8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WebGlyph(24.dp)
                Spacer(Modifier.width(6.dp))
                T("All apps", 24.sp, color = C.red, family = Bangers, weight = 400, tracking = 0.04.em)
                Spacer(Modifier.width(10.dp))
                Caption("${ordered.size} on this TV", 7.2.sp)
                Spacer(Modifier.weight(1f))
                T("★ Favorites first · Menu for options · Back returns to the board", 8.2.sp, color = C.silver)
            }
            Spacer(Modifier.height(8.dp))
            if (ordered.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    T("No TV apps found yet", 12.sp, color = C.silver)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    // Room for the focus ring at the edges.
                    contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.7.dp),
                ) {
                    itemsIndexed(ordered, key = { _, it -> it.pkg }, span = { _, it -> GridItemSpan(if (it.pkg in favorites) 2 else 1) }) { i, app ->
                        // Tiles cascade in, 18 ms apart, the first time they're shown.
                        val appear = remember { androidx.compose.animation.core.Animatable(0f) }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(18L * i.coerceAtMost(20))
                            appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.75f, stiffness = 420f))
                        }
                        BoardTile(
                            app, app.pkg in favorites, custom, art.art[app.pkg], ui,
                            Modifier.height(TILE_HEIGHT)
                                .graphicsLayer {
                                    alpha = appear.value.coerceIn(0f, 1f)
                                    translationY = (1f - appear.value) * 18.dp.toPx()
                                }
                                .focusRequester(reqFor(app.pkg))
                                .onFocusChanged { if (it.isFocused) lastFocus[0] = app.pkg },
                            onMenu = { onMenu(app.pkg) },
                        ) { onLaunch(app.pkg) }
                    }
                }
            }
            Footer(ui)
        }
    }
}

@Composable
private fun BoardTile(
    app: InstalledApp, favorite: Boolean, custom: Boolean, artwork: AppArt?, ui: UiMessages, modifier: Modifier,
    onMenu: () -> Unit, onClick: () -> Unit,
) {
    val brand = BRAND_BY_PKG[app.pkg]
    val name = brand?.name ?: app.label
    val label = "$name${if (favorite) " (favorite)" else ""} — OK to open, Menu for options"
    if (brand != null) {
        val style = BRAND_STYLES.getValue(brand.key)
        val labelColor = if (custom) C.text else style.label
        Panel(label, modifier, if (custom) C.suit else style.bg, { ui.focused = it }, onMenu, onClick) {
            if (custom) BrandArt(brand.key, style.customInk, webSize = 30.dp)
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = if (custom) Alignment.TopStart else Alignment.Center) {
                    if (custom) {
                        MarkPlate(Modifier.fillMaxHeight(0.78f).aspectRatio(1.7f, matchHeightConstraintsFirst = true)) {
                            BrandMark(brand.key, custom, Modifier.fillMaxSize())
                        }
                    } else {
                        BrandMark(brand.key, custom, Modifier.fillMaxWidth().fillMaxHeight(0.8f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (favorite) T("★ ", 9.6.sp, color = if (custom) C.red else labelColor, weight = 700)
                    T(name, 9.6.sp, color = labelColor, weight = 700)
                }
            }
        }
    } else {
        Panel(label, modifier, C.surface, { ui.focused = it }, onMenu, onClick) {
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (artwork != null) {
                        Image(
                            artwork.bitmap, null,
                            if (artwork.isBanner) Modifier.fillMaxSize() else Modifier.fillMaxHeight(0.8f).aspectRatio(1f),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        // Generic tile while loading or if the app has no artwork.
                        T(name.take(1).uppercase(), 22.sp, color = C.silver, family = Bangers, weight = 400)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (favorite) T("★ ", 9.6.sp, color = C.red, weight = 700)
                    T(name, 9.6.sp, weight = 700)
                }
            }
        }
    }
}
