package com.spidervision.tv.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.spidervision.tv.BuildConfig
import com.spidervision.tv.R
import com.spidervision.tv.apps.AppCatalog
import com.spidervision.tv.apps.BRAND_BY_PKG
import com.spidervision.tv.apps.Brand
import com.spidervision.tv.apps.InstalledApp
import com.spidervision.tv.apps.Move
import com.spidervision.tv.apps.groupPosition
import com.spidervision.tv.apps.moveApp
import com.spidervision.tv.apps.orderedApps
import com.spidervision.tv.network.DnsStatus
import com.spidervision.tv.network.DnsStatusReader
import com.spidervision.tv.network.FetchResult
import com.spidervision.tv.network.KeyVault
import com.spidervision.tv.network.NetView
import com.spidervision.tv.network.NetworkStatusCache
import com.spidervision.tv.network.NextDnsClient
import com.spidervision.tv.profiles.DEFAULT_PROFILES
import com.spidervision.tv.profiles.LauncherStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/*
 * The SpiderVision home board, laid out like the approved Orrery Comic Edition comic page.
 * All sizes are the prototype's 1920-px CSS values halved (see Tokens.kt).
 */

private val PANEL_SHAPE = RoundedCornerShape(2.dp)
private val PAD = 11.5.dp
private val ART_HEIGHT = 56.dp

// Watch panels, filled by app order (favourites first). Indices 1, 3, 4, 5 are one row tall.
private val WATCH_CELLS = listOf(
    Cell(5, 7, 1, 3), Cell(7, 10, 1, 2), Cell(7, 8, 2, 4), Cell(8, 10, 2, 3), Cell(8, 10, 3, 4), Cell(5, 7, 3, 4),
)
private val SHORT_SLOTS = setOf(1, 3, 4, 5)
private val FAV_CELLS = listOf(Cell(5, 7, 4, 6), Cell(7, 10, 4, 5), Cell(7, 10, 5, 6), Cell(5, 10, 6, 7))
private val PANEL_IDS = listOf("cover", "net", "settings", "inputs", "allapps") + (0..5).map { "w$it" } + (0..3).map { "f$it" }

@Composable
fun T(
    text: String, size: TextUnit, modifier: Modifier = Modifier, color: Color = C.text, weight: Int = 500,
    family: FontFamily = Manrope, tracking: TextUnit = TextUnit.Unspecified, maxLines: Int = 1, lineHeight: TextUnit = TextUnit.Unspecified,
) {
    val scale = LocalTextScale.current
    BasicText(
        text, modifier,
        style = TextStyle(
            color = color, fontSize = size * scale, fontWeight = FontWeight(weight), fontFamily = family, letterSpacing = tracking,
            lineHeight = if (lineHeight == TextUnit.Unspecified) lineHeight else lineHeight * scale,
        ),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    )
}

private fun Modifier.at(c: Cell) = cell(c.colStart, c.colEnd, c.rowStart, c.rowEnd)

/** Footer text: the selected item, or a short-lived message. Its own scope so focus moves don't recompose the board. */
class UiMessages {
    var focused by mutableStateOf("")
    var status by mutableStateOf<String?>(null)
}

/** Name the household sees for an app: the designed brand name, else the TV's own label. */
fun appName(pkg: String, installed: Map<String, InstalledApp>) = BRAND_BY_PKG[pkg]?.name ?: installed[pkg]?.label ?: "This app"

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    val store = remember { LauncherStore(context.applicationContext) }
    val saved by store.prefs.collectAsState(initial = null)

    var installed by remember { mutableStateOf<Map<String, InstalledApp>>(emptyMap()) }
    var net by remember { mutableStateOf(NetView(DnsStatus("", ""), linked = false, stats = null, error = null)) }
    var netTick by remember { mutableStateOf(0) }
    var showNetwork by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showAllApps by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val ui = remember { UiMessages() }
    val art = remember { ArtCache() }
    val artPx = with(LocalDensity.current) { ART_HEIGHT.roundToPx() }

    val req = remember { PANEL_IDS.associateWith { FocusRequester() } }
    val chipReq = remember { DEFAULT_PROFILES.associate { it.id to FocusRequester() } }
    // Last focused home panel; plain (non-state) so focus moves never trigger recomposition.
    val lastFocus = remember { arrayOf("cover") }
    val restoreFocus: suspend () -> Unit = {
        withFrameNanos { } // let the board compose first
        val r = req[lastFocus[0]] ?: req.getValue("cover")
        if (!runCatching { r.requestFocus() }.getOrDefault(false)) runCatching { req.getValue("cover").requestFocus() }
    }

    // Refresh the real app list, DNS mode and clock only while the screen is in front.
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            installed = AppCatalog.loadInstalled(context)
            if (!showAllApps && optionsFor == null && !showSettings && !showNetwork) restoreFocus()
            while (true) {
                now = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }

    // NextDNS: refresh only while the screen is in front; cache totals; back off on failure.
    LaunchedEffect(netTick) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var retry = 2 * 60_000L
            var force = netTick > 0
            while (true) {
                val dns = DnsStatusReader.read(context)
                val linked = KeyVault.isLinked(context)
                val profileId = dns.nextDnsProfile
                if (!linked || profileId == null) {
                    net = NetView(dns, linked, null, if (linked) "The TV's Private DNS isn't set to NextDNS" else null)
                    delay(15 * 60_000L); continue
                }
                val cached = net.stats ?: NetworkStatusCache.load(context)
                val age = cached?.let { System.currentTimeMillis() - it.fetchedAt }
                if (age != null && age < 10 * 60_000L && !force) {
                    net = NetView(dns, true, cached, null)
                    delay(10 * 60_000L - age); continue
                }
                force = false
                net = NetView(dns, true, cached, null, loading = true)
                val key = withContext(Dispatchers.IO) { KeyVault.load(context) }
                val result = if (key == null) FetchResult.Failed("The saved key couldn't be read; link NextDNS again", authProblem = true)
                else withContext(Dispatchers.IO) { NextDnsClient.fetch(profileId, key) }
                when (result) {
                    is FetchResult.Ok -> {
                        NetworkStatusCache.save(context, result.stats)
                        net = NetView(dns, true, result.stats, null)
                        retry = 2 * 60_000L
                        delay(10 * 60_000L)
                    }
                    is FetchResult.Failed -> {
                        net = NetView(dns, true, cached, result.message, result.authProblem)
                        // A bad key won't fix itself: wait for the next visit or a manual refresh.
                        delay(if (result.authProblem) 60 * 60_000L else retry)
                        retry = (retry * 2).coerceAtMost(60 * 60_000L)
                    }
                }
            }
        }
    }

    BackHandler(enabled = showAllApps) { showAllApps = false }
    LaunchedEffect(showAllApps) { if (!showAllApps) restoreFocus() }

    val prefs = saved ?: run { Box(Modifier.fillMaxSize().background(C.bg)); return }
    val active = prefs.active
    val pp = prefs.current
    val profile = DEFAULT_PROFILES.first { it.id == active }
    val ordered = remember(installed, pp) { orderedApps(installed, pp) }
    val watchApps = ordered.take(6)
    LaunchedEffect(watchApps, pp.pins, installed) {
        art.ensure(context, watchApps + pp.pins.mapNotNull { installed[it] }, artPx)
    }

    val launch = { pkg: String -> ui.status = AppCatalog.launch(context, pkg, appName(pkg, installed)) }
    val onFocusLabel = { s: String -> ui.focused = s }
    val openOptions = { pkg: String -> optionsFor = pkg }

    CompositionLocalProvider(LocalTextScale provides if (pp.largeText) 1.15f else 1f) {
        if (showAllApps) {
            AllAppsBoard(ordered, pp.favorites, pp.customIcons, art, ui, onLaunch = launch, onMenu = openOptions)
        } else {
            // Board cells → deterministic D-pad neighbours.
            val cells = buildMap {
                put("cover", Cell(1, 5, 1, 7))
                watchApps.indices.forEach { put("w$it", WATCH_CELLS[it]) }
                FAV_CELLS.forEachIndexed { i, c -> put("f$i", c) }
                put("net", Cell(10, 13, 1, 4))
                put("settings", Cell(10, 12, 4, 6))
                put("inputs", Cell(12, 13, 4, 6))
                put("allapps", Cell(10, 13, 6, 7))
            }
            val neighbors = computeNeighbors(cells)
            fun Modifier.nav(id: String): Modifier {
                val c = cells.getValue(id)
                val n = neighbors.getValue(id)
                return this.at(c)
                    .focusRequester(req.getValue(id))
                    .focusProperties {
                        left = n.left?.let(req::getValue) ?: FocusRequester.Cancel
                        right = n.right?.let(req::getValue) ?: FocusRequester.Cancel
                        up = n.up?.let(req::getValue) ?: if (c.rowStart == 1) chipReq.getValue(active) else FocusRequester.Cancel
                        down = n.down?.let(req::getValue) ?: FocusRequester.Cancel
                    }
                    .onFocusChanged { if (it.isFocused) lastFocus[0] = id }
            }

            Box(Modifier.fillMaxSize().background(C.bg)) {
                Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.5f).fillMaxHeight(0.34f).halftone(C.halftonePage, 5.5.dp, 1.2.dp))
                Column(Modifier.fillMaxSize().padding(start = 28.8.dp, end = 28.8.dp, top = 16.dp, bottom = 10.8.dp)) {
                    // ---------- Header ----------
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WebGlyph(28.8.dp)
                        Spacer(Modifier.width(6.dp))
                        Column {
                            T("SpiderVision", 27.8.sp, color = C.red, family = Bangers, weight = 400, tracking = 0.04.em, lineHeight = 24.sp)
                            Spacer(Modifier.height(2.dp))
                            T(
                                "LIVING ROOM · VOL. 1", 6.sp, weight = 800, tracking = 0.14.em,
                                modifier = Modifier.skewedBox(C.blue).padding(horizontal = 4.dp, vertical = 0.5.dp),
                            )
                        }
                        Spacer(Modifier.width(23.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            DEFAULT_PROFILES.forEachIndexed { i, p ->
                                val chipNav = Modifier
                                    .focusRequester(chipReq.getValue(p.id))
                                    .focusProperties {
                                        up = FocusRequester.Cancel
                                        down = req.getValue("cover")
                                        if (i == 0) left = FocusRequester.Cancel
                                        if (i == DEFAULT_PROFILES.lastIndex) right = FocusRequester.Cancel
                                    }
                                ProfileChip(p.name, p.color, p.id == active, onFocusLabel, chipNav) {
                                    scope.launch { store.setActive(p.id) }
                                    ui.status = "Switched to ${p.name}"
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            T(now.format(DateTimeFormatter.ofPattern("h:mm a")), 24.sp, family = Bangers, weight = 400, tracking = 0.04.em)
                            Spacer(Modifier.height(3.dp))
                            val hour = now.hour
                            val greeting = when { hour < 5 -> "Good night"; hour < 12 -> "Good morning"; hour < 18 -> "Good afternoon"; else -> "Good evening" }
                            T("$greeting, ${profile.name} · ${now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))}", 8.2.sp, color = C.silver)
                        }
                    }
                    Spacer(Modifier.height(13.dp))

                    // ---------- Comic page ----------
                    BoardGrid(12, 6, 7.7.dp, Modifier.weight(1f).fillMaxWidth()) {
                        CoverPanel(pp.cover, installed, profile.name, Modifier.nav("cover"), onFocusLabel, { openOptions(pp.cover) }) { launch(pp.cover) }

                        watchApps.forEachIndexed { i, app ->
                            WatchPanel(app, i, pp.customIcons, art.art[app.pkg], Modifier.nav("w$i"), onFocusLabel, { openOptions(app.pkg) }) { launch(app.pkg) }
                        }

                        pp.pins.forEachIndexed { i, pkg ->
                            PinPanel(pkg, installed, i, profile.name, pp.customIcons, art.art[pkg], Modifier.nav("f$i"), onFocusLabel, { openOptions(pkg) }) { launch(pkg) }
                        }

                        NetworkPanel(net, Modifier.nav("net"), onFocusLabel) { showNetwork = true }
                        Panel("Settings — icons, text size and ${profile.name}'s layout", Modifier.nav("settings"), C.surface, onFocusLabel, onClick = { showSettings = true }) {
                            Column(Modifier.fillMaxSize().padding(PAD), verticalArrangement = Arrangement.SpaceBetween) {
                                StrokeIcon(Icons.gear, C.silver, 19.2.dp)
                                Column {
                                    T("Settings", 14.4.sp, weight = 700)
                                    Spacer(Modifier.height(3.dp))
                                    T("Display, icons, profiles", 8.2.sp, color = C.silver)
                                }
                            }
                        }
                        Panel("Inputs — press OK to choose HDMI or another TV source", Modifier.nav("inputs"), C.surface, onFocusLabel, onClick = { ui.status = AppCatalog.openInputPicker(context) }) {
                            Column(Modifier.fillMaxSize().padding(PAD), verticalArrangement = Arrangement.SpaceBetween) {
                                StrokeIcon(Icons.tv, C.silver, 19.2.dp)
                                Column {
                                    T("Inputs", 11.5.sp, weight = 700)
                                    Spacer(Modifier.height(3.dp))
                                    T("HDMI & TV", 7.7.sp, color = C.silver)
                                }
                            }
                        }
                        Panel("All apps — press OK to see every app on this TV", Modifier.nav("allapps"), C.surface, onFocusLabel, onClick = { showAllApps = true }) {
                            Row(Modifier.fillMaxSize().padding(horizontal = PAD, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                StrokeIcon(Icons.grid, C.silver, 17.3.dp)
                                Spacer(Modifier.width(8.dp))
                                T("All apps", 12.5.sp, weight = 700)
                                Spacer(Modifier.weight(1f))
                                T("${installed.size} installed", 8.2.sp, color = C.silver)
                            }
                        }
                    }

                    Footer(ui)
                }
            }
        }

        // ---------- Sheets ----------
        optionsFor?.let { pkg ->
            val name = appName(pkg, installed)
            val isInstalled = installed.containsKey(pkg)
            val isFav = pkg in pp.favorites
            val (pos, size) = groupPosition(ordered, pp.favorites, pkg)
            val update = { change: (com.spidervision.tv.profiles.ProfilePrefs) -> com.spidervision.tv.profiles.ProfilePrefs -> scope.launch { store.update(active, change) } }
            val move = { how: Move -> moveApp(ordered, pp.favorites, pkg, how)?.let { newOrder -> update { it.copy(order = newOrder) } } }
            SideSheet(
                title = "App options",
                subtitle = when {
                    !isInstalled -> "Not installed on this TV"
                    isFav -> "${profile.name}'s favorite · ${pos + 1} of $size"
                    else -> "Position ${pos + 1} of $size"
                },
                onDismiss = {
                    optionsFor = null
                    // Follow the app if it moved to another Watch panel.
                    val slot = watchApps.indexOfFirst { it.pkg == pkg }
                    if (lastFocus[0].startsWith("w") && slot >= 0) lastFocus[0] = "w$slot"
                    if (!showAllApps) scope.launch { restoreFocus() }
                },
                header = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppChip(pkg, installed, pp.customIcons, art.art[pkg], 28.dp)
                        Spacer(Modifier.width(8.dp))
                        T(name, 18.sp, family = Bangers, weight = 400, tracking = 0.04.em)
                    }
                },
            ) { first ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SheetButton(
                        if (isFav) "★ Remove from ${profile.name}'s favorites" else "★ Add to ${profile.name}'s favorites",
                        Modifier.fillMaxWidth().focusRequester(first), enabled = isInstalled, accent = !isFav && isInstalled,
                    ) {
                        update { it.copy(favorites = if (isFav) it.favorites - pkg else it.favorites + pkg) }
                        ui.status = if (isFav) "$name removed from favorites" else "$name added to favorites"
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SheetButton("◀ Earlier", Modifier.weight(1f), enabled = isInstalled && pos > 0) { move(Move.EARLIER) }
                        SheetButton("Later ▶", Modifier.weight(1f), enabled = isInstalled && pos in 0 until size - 1) { move(Move.LATER) }
                    }
                    SheetButton("Move to front", Modifier.fillMaxWidth(), enabled = isInstalled && pos > 0) { move(Move.FRONT) }
                    SheetButton(
                        if (pp.cover == pkg) "This is ${profile.name}'s cover pick" else "Make it ${profile.name}'s cover pick",
                        Modifier.fillMaxWidth(), enabled = isInstalled && pp.cover != pkg,
                    ) {
                        update { it.copy(cover = pkg) }
                        ui.status = "$name is now ${profile.name}'s pick"
                    }
                    Spacer(Modifier.height(4.dp))
                    SheetLabel("Pin to ${profile.name}'s favorites panel")
                    pp.pins.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEachIndexed { col, current ->
                                val slot = row * 2 + col
                                SheetButton(
                                    "${slot + 1} · ${appName(current, installed)}",
                                    Modifier.weight(1f), enabled = isInstalled && current != pkg,
                                ) {
                                    update { p -> p.copy(pins = p.pins.toMutableList().also { it[slot] = pkg }) }
                                    ui.status = "$name pinned to slot ${slot + 1}"
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showNetwork) {
            NetworkSheet(net, onRefresh = { netTick++ }) { showNetwork = false; scope.launch { restoreFocus() } }
        }

        if (showSettings) {
            var confirmReset by remember { mutableStateOf(false) }
            SideSheet(
                title = "Settings",
                subtitle = "These apply to ${profile.name}'s profile only.",
                onDismiss = { showSettings = false; scope.launch { restoreFocus() } },
                header = { T("${profile.name}'s SpiderVision", 18.sp, family = Bangers, weight = 400, tracking = 0.04.em) },
            ) { first ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwitchRow("Custom app icons", "Unified dark tiles with web details. Off shows each app's own colours.", pp.customIcons, Modifier.focusRequester(first)) {
                        scope.launch { store.update(active) { it.copy(customIcons = !it.customIcons) } }
                    }
                    SwitchRow("Larger text", "For reading from farther away.", pp.largeText) {
                        scope.launch { store.update(active) { it.copy(largeText = !it.largeText) } }
                    }
                    Spacer(Modifier.height(4.dp))
                    SheetLabel("Tip")
                    T("Press Menu on the remote (or hold OK) on any app to favorite, move or pin it.", 8.2.sp, color = C.silver, maxLines = 3, lineHeight = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    SheetButton("Open TV settings", Modifier.fillMaxWidth()) {
                        ui.status = AppCatalog.launch(context, "com.android.tv.settings", "TV settings")
                    }
                    SheetButton(
                        if (confirmReset) "Press again to reset ${profile.name}" else "Reset ${profile.name}'s layout",
                        Modifier.fillMaxWidth(), accent = confirmReset,
                    ) {
                        if (confirmReset) {
                            scope.launch { store.reset(active) }
                            ui.status = "${profile.name}'s layout is back to the defaults"
                            confirmReset = false
                        } else confirmReset = true
                    }
                    Spacer(Modifier.height(6.dp))
                    T("SpiderVision ${BuildConfig.VERSION_NAME} · Orrery Comic Edition design", 7.2.sp, color = C.silver)
                }
            }
        }
    }
}

/** Divider + selected-item line + remote key legend. */
@Composable
fun Footer(ui: UiMessages) {
    LaunchedEffect(ui.status) {
        if (ui.status != null) { delay(3200); ui.status = null }
    }
    Spacer(Modifier.height(10.8.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.rule))
    Spacer(Modifier.height(8.6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            val s = ui.status
            if (s != null) SpeechBubble(s) else T(ui.focused, 8.6.sp, weight = 600)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Key("◀ ▲ ▼ ▶"); T("Move", 8.6.sp, color = C.silver)
            Key("OK"); T("Open", 8.6.sp, color = C.silver)
            Key("Menu"); T("Options", 8.6.sp, color = C.silver)
            Key("Back"); T("Close", 8.6.sp, color = C.silver)
        }
    }
}

/**
 * A focusable comic panel: cream border, flat fill, yellow ring when selected.
 * [onMenu] runs on the remote's Menu key or a long press of OK.
 */
@Composable
fun Panel(
    label: String, modifier: Modifier, bg: Color, onFocusLabel: (String) -> Unit,
    onMenu: (() -> Unit)? = null, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused, label) { if (focused) onFocusLabel(label) }
    Box(
        modifier
            .focusRing(focused)
            .onPreviewKeyEvent { e ->
                if (onMenu != null && e.key == Key.Menu) {
                    if (e.type == KeyEventType.KeyUp) onMenu()
                    true
                } else false
            }
            .combinedClickable(interactionSource = source, indication = null, onLongClick = onMenu, onClick = onClick)
            .clip(PANEL_SHAPE)
            .background(bg)
            .border(1.5.dp, C.cream, PANEL_SHAPE),
        content = content,
    )
}

@Composable
private fun CoverPanel(pkg: String, installed: Map<String, InstalledApp>, profileName: String, modifier: Modifier, onFocusLabel: (String) -> Unit, onMenu: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    // Decoded once at near display size, 16-bit, to keep memory low.
    val coverArt = remember {
        BitmapFactory.decodeResource(
            context.resources, R.drawable.cover_collage,
            BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 },
        ).asImageBitmap()
    }
    val name = appName(pkg, installed)
    val isInstalled = installed.containsKey(pkg)
    val label = if (isInstalled) "$profileName's pick: $name — press OK to open" else "$profileName's pick: $name isn't installed on this TV"
    Panel(label, modifier, C.ink, onFocusLabel, onMenu, onClick) {
        Image(coverArt, null, Modifier.matchParentSize(), alignment = BiasAlignment(0f, -0.64f), contentScale = ContentScale.Crop)
        Box(
            Modifier.matchParentSize().drawBehind {
                val top = size.height * 0.48f
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.66f to C.ink, 1f to C.ink, startY = top, endY = size.height),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, size.height - top),
                )
            },
        )
        Caption("Tonight's issue", 7.7.sp, Modifier.align(Alignment.TopStart).padding(7.7.dp))
        Burst("Play!", 53.8.dp, 9.6.sp, Modifier.align(Alignment.TopEnd).padding(4.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(14.4.dp)) {
            Caption("$profileName's pick", 7.2.sp)
            Spacer(Modifier.height(4.dp))
            T(name, 34.6.sp, family = Bangers, weight = 400, tracking = 0.04.em, lineHeight = 32.sp)
            Spacer(Modifier.height(4.dp))
            T(if (isInstalled) "Press OK to open $name" else "$name isn't installed", 10.1.sp, color = if (isInstalled) C.silver else C.red, weight = 600)
        }
    }
}

/** The app's mark (designed brand) or its own banner/icon, fitted into the given box. */
@Composable
private fun AppMark(pkg: String, custom: Boolean, art: AppArt?, modifier: Modifier, fallbackName: String) {
    val brand: Brand? = BRAND_BY_PKG[pkg]
    when {
        brand != null -> BrandMark(brand.key, custom, modifier)
        art != null -> Image(art.bitmap, null, modifier, contentScale = ContentScale.Fit)
        else -> Box(modifier, contentAlignment = Alignment.Center) { T(fallbackName.take(1).uppercase(), 18.sp, color = C.silver, family = Bangers, weight = 400) }
    }
}

@Composable
private fun WatchPanel(app: InstalledApp, slot: Int, custom: Boolean, art: AppArt?, modifier: Modifier, onFocusLabel: (String) -> Unit, onMenu: () -> Unit, onClick: () -> Unit) {
    val brand = BRAND_BY_PKG[app.pkg]
    val style = brand?.let { BRAND_STYLES.getValue(it.key) }
    val name = brand?.name ?: app.label
    val brandTile = style != null
    val labelColor = if (custom || style == null) C.text else style.label
    val bg = when { style == null -> C.surface; custom -> C.suit; else -> style.bg }
    Panel("$name — press OK to open", modifier, bg, onFocusLabel, onMenu, onClick) {
        if (custom && brandTile) WebCorner(C.webOnTile, Modifier.align(Alignment.TopEnd).fillMaxHeight(0.3f).heightIn(max = 42.dp).aspectRatio(1f))
        if (slot in SHORT_SLOTS) {
            Row(Modifier.fillMaxSize().padding(horizontal = PAD), verticalAlignment = Alignment.CenterVertically) {
                T(name, 12.5.sp, Modifier.weight(1f), color = labelColor, weight = 700)
                Spacer(Modifier.width(6.dp))
                AppMark(app.pkg, custom, art, Modifier.fillMaxHeight(0.5f).aspectRatio(160f / 90f, matchHeightConstraintsFirst = true), name)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(PAD)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AppMark(app.pkg, custom, art, Modifier.fillMaxWidth().fillMaxHeight(if (slot == 2) 0.6f else 0.52f), name)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    if (slot == 2) T(name, 9.1.sp, Modifier.weight(1f), color = labelColor, weight = 700, maxLines = 2)
                    else T(name, 12.5.sp, Modifier.weight(1f), color = labelColor, weight = 700)
                    if (slot == 0 && brand != null) T(brand.tag.uppercase(), 6.7.sp, color = if (custom) C.silver else labelColor, weight = 600, tracking = 0.12.em)
                }
            }
        }
        if (slot == 0) Caption("Watch", 7.2.sp, Modifier.align(Alignment.TopStart))
    }
}

/** Small app badge used inside Favorites panels and sheets. */
@Composable
fun AppChip(pkg: String, installed: Map<String, InstalledApp>, custom: Boolean, art: AppArt?, height: Dp) {
    val style = BRAND_BY_PKG[pkg]?.let { BRAND_STYLES.getValue(it.key) }
    val bg = when { style == null -> C.surface; custom -> C.suit; else -> style.bg }
    Box(
        Modifier.height(height).aspectRatio(1.6f).clip(RoundedCornerShape(3.dp)).background(bg),
        contentAlignment = Alignment.Center,
    ) { AppMark(pkg, custom, art, Modifier.fillMaxSize(if (style != null) 0.7f else 0.92f), appName(pkg, installed)) }
}

/** A Favorites panel: an honest app shortcut (no fake titles or progress). */
@Composable
private fun PinPanel(
    pkg: String, installed: Map<String, InstalledApp>, index: Int, profileName: String, custom: Boolean, art: AppArt?,
    modifier: Modifier, onFocusLabel: (String) -> Unit, onMenu: () -> Unit, onClick: () -> Unit,
) {
    val name = appName(pkg, installed)
    val isInstalled = installed.containsKey(pkg)
    val label = if (isInstalled) "$name — press OK to open" else "$name isn't installed on this TV"
    val sub = when { !isInstalled -> "Not installed"; else -> BRAND_BY_PKG[pkg]?.tag ?: "App" }
    Panel(label, modifier, C.surface, onFocusLabel, onMenu, onClick) {
        if (index == 0) {
            Box(Modifier.align(Alignment.BottomEnd).fillMaxHeight(0.85f).fillMaxWidth(0.65f).halftone(C.halftonePanel, 4.5.dp, 1.dp))
            Column(Modifier.fillMaxSize().padding(PAD), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppChip(pkg, installed, custom, art, 26.dp)
                    Spacer(Modifier.width(6.dp))
                    T("App shortcut", 7.7.sp, color = C.silver, weight = 600)
                }
                Column {
                    T(name, 15.4.sp, family = Bangers, weight = 400, tracking = 0.04.em)
                    Spacer(Modifier.height(3.dp))
                    T(sub, 8.2.sp, color = if (isInstalled) C.silver else C.red)
                }
            }
            Caption("$profileName's favorites", 7.2.sp, Modifier.align(Alignment.TopStart))
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 9.6.dp, vertical = 5.4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.6.dp),
            ) {
                AppChip(pkg, installed, custom, art, 26.dp)
                Column(Modifier.weight(1f)) {
                    T(name, 12.sp, family = Bangers, weight = 400, tracking = 0.04.em)
                    Spacer(Modifier.height(1.dp))
                    T("App shortcut", 7.7.sp, color = C.silver)
                }
                T(sub, 8.2.sp, color = if (isInstalled) C.silver else C.red, weight = 600)
            }
        }
    }
}

private val timeFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private fun Long.clock(): String = java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).format(timeFmt)
private fun Long.grouped(): String = java.text.NumberFormat.getIntegerInstance().format(this)
private fun Double.percent(): String = String.format(java.util.Locale.US, "%.1f%%", this)

@Composable
private fun NetworkPanel(net: NetView, modifier: Modifier, onFocusLabel: (String) -> Unit, onClick: () -> Unit) {
    val stats = net.stats
    val badge = when {
        !net.linked -> "NOT LINKED"
        net.error != null -> if (net.authProblem) "CHECK KEY" else "OFFLINE"
        net.loading && stats == null -> "LOADING"
        else -> "LIVE · 24 H"
    }
    val live = badge.startsWith("LIVE")
    Panel("Home base — press OK for NextDNS details", modifier, C.surface, onFocusLabel, onClick = onClick) {
        Column(Modifier.fillMaxSize().padding(start = PAD, end = PAD, bottom = PAD, top = 25.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StrokeIcon(Icons.shield, C.blue, 13.4.dp, strokeWidth = 1.8f)
                Spacer(Modifier.width(5.dp))
                T(net.dns.title, 10.6.sp, weight = 700)
                Spacer(Modifier.weight(1f))
                T(
                    badge, 6.2.sp, color = if (live) C.text else C.silver, weight = 600, tracking = 0.08.em,
                    modifier = Modifier.clip(CircleShape).background(if (live) C.blue else C.navy).padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
            Spacer(Modifier.height(7.dp))
            if (stats != null) {
                Row {
                    NetStat("Queries", stats.queries.grouped(), Modifier.weight(1f))
                    NetStat("Blocked", stats.blockedPercent.percent(), Modifier.weight(1f), C.red)
                    NetStat("Blocked #", stats.blocked.grouped(), Modifier.weight(1f))
                }
                val err = net.error
                if (err != null) {
                    Spacer(Modifier.height(3.dp))
                    T("$err · showing ${stats.fetchedAt.clock()} numbers", 7.sp, color = C.red, maxLines = 2)
                }
            } else {
                T(
                    when {
                        !net.linked -> "NextDNS stats not connected"
                        net.error != null -> net.error
                        else -> "Loading NextDNS stats…"
                    },
                    10.sp, weight = 700, maxLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                T(
                    if (!net.linked) "${net.dns.detail} Link it from your PC to see live counts." else "Press OK for details.",
                    7.2.sp, color = C.silver, maxLines = 3, lineHeight = 10.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            val hourly = stats?.hourly.orEmpty()
            val max = (hourly.maxOrNull() ?: 0L).coerceAtLeast(1L)
            Row(Modifier.fillMaxWidth().height(32.4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                if (hourly.isEmpty()) {
                    // Empty frame: never bars with made-up values.
                    repeat(12) { Box(Modifier.weight(1f).height(2.dp).background(C.rule)) }
                } else {
                    hourly.forEachIndexed { i, v ->
                        Box(
                            Modifier.weight(1f).fillMaxHeight((v.toFloat() / max).coerceAtLeast(0.04f))
                                .background(if (i == hourly.lastIndex) C.red else C.blue),
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row {
                T("12 h ago", 6.2.sp, color = C.silver)
                Spacer(Modifier.weight(1f))
                T(if (stats != null) "Updated ${stats.fetchedAt.clock()}" else "No data yet", 6.2.sp, color = C.silver)
                Spacer(Modifier.weight(1f))
                T("Now", 6.2.sp, color = C.silver)
            }
        }
        Caption("Home base", 7.2.sp, Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun NetStat(label: String, value: String, modifier: Modifier, color: Color = C.text) {
    Column(modifier) {
        T(label.uppercase(), 6.7.sp, color = C.silver, weight = 600, tracking = 0.1.em)
        Spacer(Modifier.height(1.dp))
        T(value, 11.sp, color = color, weight = 700)
    }
}

/** Home base details: honest wording about what the numbers are, plus link status. */
@Composable
private fun NetworkSheet(net: NetView, onRefresh: () -> Unit, onDismiss: () -> Unit) {
    val stats = net.stats
    val profileId = net.dns.nextDnsProfile
    SideSheet(
        title = "Home base",
        subtitle = if (profileId != null) "NextDNS profile $profileId · this TV's Private DNS" else net.dns.detail,
        onDismiss = onDismiss,
        header = { T(net.dns.title, 18.sp, family = Bangers, weight = 400, tracking = 0.04.em) },
    ) { first ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (stats != null) {
                SheetLabel("Last 24 hours")
                Row {
                    NetStat("Queries", stats.queries.grouped(), Modifier.weight(1f))
                    NetStat("Blocked", stats.blocked.grouped(), Modifier.weight(1f), C.red)
                    NetStat("Rate", stats.blockedPercent.percent(), Modifier.weight(1f), C.red)
                }
                val err = net.error
                T(
                    "Updated ${stats.fetchedAt.clock()}" + (if (err != null) " · $err" else ""),
                    7.7.sp, color = if (err != null) C.red else C.silver, maxLines = 2,
                )
                if (stats.topBlocked.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    SheetLabel("Most blocked")
                    stats.topBlocked.forEach { (domain, n) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            T(domain, 8.6.sp, Modifier.weight(1f))
                            T(n.grouped(), 8.6.sp, color = C.silver)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                T(
                    "Counts are DNS lookups from devices using this NextDNS profile. A blocked lookup isn't the same as a blocked ad.",
                    7.2.sp, color = C.silver, maxLines = 3, lineHeight = 10.sp,
                )
            } else {
                T(
                    when {
                        !net.linked -> "Live stats aren't linked."
                        net.error != null -> net.error
                        else -> "Loading…"
                    },
                    10.sp, weight = 700, maxLines = 2,
                )
            }
            Spacer(Modifier.height(4.dp))
            SheetButton("Refresh now", Modifier.fillMaxWidth().focusRequester(first), enabled = net.linked) { onRefresh() }
            SheetLabel(if (net.linked) "To unlink" else "To link")
            T(
                if (net.linked) "On your PC run tools\\link-nextdns.ps1 -Unlink. The key is stored encrypted on this TV only."
                else "On your PC run tools\\link-nextdns.ps1 and paste your NextDNS API key when asked. It stays between your PC and this TV.",
                7.7.sp, color = C.silver, maxLines = 4, lineHeight = 11.sp,
            )
        }
    }
}

@Composable
private fun ProfileChip(name: String, color: Color, active: Boolean, onFocusLabel: (String) -> Unit, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { if (focused) onFocusLabel("Switch to $name's profile") }
    Row(
        modifier
            .focusRing(focused, corner = 20.dp)
            .combinedClickable(interactionSource = source, indication = null, onClick = onClick)
            .clip(CircleShape)
            .background(if (active) C.text else C.surface)
            .padding(start = 3.8.dp, top = 3.8.dp, bottom = 3.8.dp, end = 9.6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.2.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            T(name.take(1), 8.2.sp, color = C.suit, weight = 700)
        }
        Spacer(Modifier.width(5.dp))
        T(name, 9.6.sp, color = if (active) C.bg else C.silver, weight = 600)
    }
}

@Composable
private fun SpeechBubble(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(width = 7.dp, height = 7.dp)) {
            val p = Path().apply { moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height) }
            drawPath(p, C.cream)
            drawPath(p, C.ink, style = Stroke(1.dp.toPx()))
        }
        T(
            text, 8.6.sp, color = C.ink, weight = 800,
            modifier = Modifier.clip(CircleShape).background(C.cream).border(1.dp, C.ink, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun Key(label: String) {
    T(
        label, 7.7.sp, weight = 600,
        modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(C.surface).border(0.5.dp, C.rule, RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp).widthIn(min = 8.dp),
    )
}

/** Solid −6° skewed strip behind text (masthead "Living room" label). */
private fun Modifier.skewedBox(color: Color) = drawBehind {
    val dx = size.height / 2 * 0.105f
    val p = Path().apply { moveTo(dx, 0f); lineTo(size.width + dx, 0f); lineTo(size.width - dx, size.height); lineTo(-dx, size.height); close() }
    drawPath(p, color)
}
