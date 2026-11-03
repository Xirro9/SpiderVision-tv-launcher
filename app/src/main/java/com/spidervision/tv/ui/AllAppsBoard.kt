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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spidervision.tv.apps.AppCatalog
import com.spidervision.tv.apps.BRAND_BY_KEY
import com.spidervision.tv.apps.BRAND_BY_PKG
import com.spidervision.tv.apps.Brand
import com.spidervision.tv.apps.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BoardEntry(val app: InstalledApp, val brand: Brand?, val favorite: Boolean) {
    val name get() = brand?.name ?: app.label
}

private val TILE_HEIGHT = 80.dp
private val ART_HEIGHT = 56.dp

/**
 * Every app the TV exposes, in one comic-style board. Designed brands use their marks
 * (favourites first, as wide panels); every other app shows its own TV banner or icon.
 * Back closes the board (handled by the caller).
 */
@Composable
fun AllAppsBoard(
    installed: Map<String, InstalledApp>, appOrder: List<String>, favorites: List<String>,
    custom: Boolean, ui: UiMessages, onLaunch: (InstalledApp) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current

    val entries = remember(installed, appOrder, favorites) {
        val brands = appOrder.mapNotNull { BRAND_BY_KEY[it] }
            .mapNotNull { b -> installed[b.pkg]?.let { BoardEntry(it, b, b.key in favorites) } }
            .sortedByDescending { it.favorite } // stable: keeps app order within each group
        val others = installed.values.filter { BRAND_BY_PKG[it.pkg] == null }
            .sortedBy { it.label.lowercase() }.map { BoardEntry(it, null, false) }
        brands + others
    }

    // Banners for non-brand apps: rendered at tile size off the main thread, dropped when the board closes.
    val art = remember { mutableStateMapOf<String, Pair<ImageBitmap, Boolean>>() }
    LaunchedEffect(entries) {
        val hPx = with(density) { ART_HEIGHT.roundToPx() }
        val wPx = hPx * 16 / 9
        for (e in entries) {
            if (e.brand != null || e.app.pkg in art) continue
            val loaded = withContext(Dispatchers.IO) { AppCatalog.loadArtwork(context, e.app, wPx, hPx) }
            if (loaded != null) art[e.app.pkg] = loaded
        }
    }

    val reqs = remember(entries) { entries.associate { it.app.pkg to FocusRequester() } }
    val lastFocus = remember { arrayOf<String?>(null) }
    // Focus the first app on open, and the last-used app when coming back from it.
    LaunchedEffect(entries) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withFrameNanos { }
            val target = lastFocus[0]?.let { reqs[it] } ?: entries.firstOrNull()?.let { reqs[it.app.pkg] }
            runCatching { target?.requestFocus() }
        }
    }

    Box(Modifier.fillMaxSize().background(C.bg)) {
        Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.5f).fillMaxHeight(0.34f).halftone(C.halftonePage, 5.5.dp, 1.2.dp))
        Column(Modifier.fillMaxSize().padding(start = 28.8.dp, end = 28.8.dp, top = 16.dp, bottom = 10.8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WebGlyph(24.dp)
                Spacer(Modifier.width(6.dp))
                T("All apps", 24.sp, color = C.red, family = Bangers, weight = 400, tracking = 0.04.em)
                Spacer(Modifier.width(10.dp))
                Caption("${entries.size} on this TV", 7.2.sp)
                Spacer(Modifier.weight(1f))
                T("★ Favorites first · Back returns to the board", 8.2.sp, color = C.silver)
            }
            Spacer(Modifier.height(8.dp))
            if (entries.isEmpty()) {
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
                    items(entries, key = { it.app.pkg }, span = { GridItemSpan(if (it.favorite) 2 else 1) }) { e ->
                        BoardTile(
                            e, custom, art[e.app.pkg], ui,
                            Modifier.height(TILE_HEIGHT)
                                .focusRequester(reqs.getValue(e.app.pkg))
                                .onFocusChanged { if (it.isFocused) lastFocus[0] = e.app.pkg },
                        ) { onLaunch(e.app) }
                    }
                }
            }
            Footer(ui)
        }
    }
}

@Composable
private fun BoardTile(e: BoardEntry, custom: Boolean, artwork: Pair<ImageBitmap, Boolean>?, ui: UiMessages, modifier: Modifier, onClick: () -> Unit) {
    val label = "${e.name}${if (e.favorite) " (favorite)" else ""} — press OK to open"
    val brand = e.brand
    if (brand != null) {
        val style = BRAND_STYLES.getValue(brand.key)
        val labelColor = if (custom) C.text else style.label
        Panel(label, modifier, if (custom) C.suit else style.bg, { ui.focused = it }, onClick) {
            if (custom) WebCorner(C.webOnTile, Modifier.align(Alignment.TopEnd).fillMaxHeight(0.36f).heightIn(max = 30.dp).aspectRatio(1f))
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BrandMark(brand.key, custom, Modifier.fillMaxWidth().fillMaxHeight(0.8f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (e.favorite) T("★ ", 9.6.sp, color = if (custom) C.red else labelColor, weight = 700)
                    T(e.name, 9.6.sp, color = labelColor, weight = 700)
                }
            }
        }
    } else {
        Panel(label, modifier, C.surface, { ui.focused = it }, onClick) {
            Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (artwork != null) {
                        val (bmp, isBanner) = artwork
                        Image(
                            bmp, null,
                            if (isBanner) Modifier.fillMaxSize() else Modifier.fillMaxHeight(0.8f).aspectRatio(1f),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        // Generic tile while loading or if the app has no artwork.
                        T(e.name.take(1).uppercase(), 22.sp, color = C.silver, family = Bangers, weight = 400)
                    }
                }
                T(e.name, 9.6.sp, weight = 700)
            }
        }
    }
}
