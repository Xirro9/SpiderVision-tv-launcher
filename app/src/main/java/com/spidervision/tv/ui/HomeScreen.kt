package com.spidervision.tv.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.spidervision.tv.R
import com.spidervision.tv.apps.AppCatalog
import com.spidervision.tv.apps.BRAND_BY_KEY
import com.spidervision.tv.apps.Brand
import com.spidervision.tv.apps.InstalledApp
import com.spidervision.tv.network.DnsStatus
import com.spidervision.tv.network.DnsStatusReader
import com.spidervision.tv.profiles.DEFAULT_APP_ORDER
import com.spidervision.tv.profiles.DEFAULT_FAVORITE_APPS
import com.spidervision.tv.profiles.DEFAULT_PROFILES
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/*
 * The SpiderVision home board, laid out like the approved Orrery Comic Edition comic page.
 * All sizes are the prototype's 1920-px CSS values halved (see Tokens.kt).
 */

private val PANEL_SHAPE = RoundedCornerShape(2.dp)
private val PAD = 11.5.dp

// Watch panels, filled by app order (favourites first). Indices 1, 3, 4, 5 are one row tall.
private val WATCH_CELLS = listOf(
    Cell(5, 7, 1, 3), Cell(7, 10, 1, 2), Cell(7, 8, 2, 4), Cell(8, 10, 2, 3), Cell(8, 10, 3, 4), Cell(5, 7, 3, 4),
)
private val SHORT_SLOTS = setOf(1, 3, 4, 5)
private val FAV_CELLS = listOf(Cell(5, 7, 4, 6), Cell(7, 10, 4, 5), Cell(7, 10, 5, 6), Cell(5, 10, 6, 7))

@Composable
fun T(
    text: String, size: TextUnit, modifier: Modifier = Modifier, color: Color = C.text, weight: Int = 500,
    family: FontFamily = Manrope, tracking: TextUnit = TextUnit.Unspecified, maxLines: Int = 1, lineHeight: TextUnit = TextUnit.Unspecified,
) = BasicText(
    text, modifier,
    style = TextStyle(color = color, fontSize = size, fontWeight = FontWeight(weight), fontFamily = family, letterSpacing = tracking, lineHeight = lineHeight),
    maxLines = maxLines, overflow = TextOverflow.Ellipsis,
)

private fun Modifier.at(c: Cell) = cell(c.colStart, c.colEnd, c.rowStart, c.rowEnd)

/** Footer text: the selected item, or a short-lived message. Its own scope so focus moves don't recompose the board. */
class UiMessages {
    var focused by mutableStateOf("")
    var status by mutableStateOf<String?>(null)
}

private val PANEL_IDS = listOf("cover", "net", "settings", "inputs", "allapps") + (0..5).map { "w$it" } + (0..3).map { "f$it" }

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var profileId by remember { mutableStateOf(DEFAULT_PROFILES.first().id) }
    var installed by remember { mutableStateOf<Map<String, InstalledApp>>(emptyMap()) }
    var dns by remember { mutableStateOf(DnsStatus("", "")) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showAllApps by remember { mutableStateOf(false) }
    val ui = remember { UiMessages() }
    val customIcons = true // icon-style switch arrives with settings in Phase 3

    val req = remember { PANEL_IDS.associateWith { FocusRequester() } }
    val chipReq = remember { DEFAULT_PROFILES.associate { it.id to FocusRequester() } }
    // Last focused home panel; plain (non-state) so focus moves never trigger recomposition.
    val lastFocus = remember { arrayOf("cover") }
    val restoreFocus: suspend () -> Unit = {
        withFrameNanos { } // let the board compose first
        val r = req[lastFocus[0]] ?: req.getValue("cover")
        if (runCatching { r.requestFocus() }.getOrDefault(false) == false) runCatching { req.getValue("cover").requestFocus() }
    }

    // Refresh the real app list, DNS mode and clock only while the screen is in front.
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            installed = AppCatalog.loadInstalled(context)
            dns = DnsStatusReader.read(context)
            if (!showAllApps) restoreFocus()
            while (true) {
                now = LocalDateTime.now()
                delay(60_000L - System.currentTimeMillis() % 60_000L)
            }
        }
    }

    BackHandler(enabled = showAllApps) { showAllApps = false }
    LaunchedEffect(showAllApps) { if (!showAllApps) restoreFocus() }

    val profile = DEFAULT_PROFILES.first { it.id == profileId }
    val isInstalled = { b: Brand -> installed.containsKey(b.pkg) }
    val appOrder = (DEFAULT_FAVORITE_APPS + DEFAULT_APP_ORDER).distinct()
    val watchApps = appOrder.mapNotNull { BRAND_BY_KEY[it] }.filter(isInstalled).take(6)
    val coverBrand = BRAND_BY_KEY.getValue(profile.coverApp)
    val launch = { b: Brand -> ui.status = AppCatalog.launch(context, b.pkg, b.name) }
    val onFocusLabel = { s: String -> ui.focused = s }

    if (showAllApps) {
        AllAppsBoard(installed, appOrder, DEFAULT_FAVORITE_APPS, customIcons, ui) { app -> ui.status = AppCatalog.launch(context, app.pkg, app.label) }
        return
    }

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
                up = n.up?.let(req::getValue) ?: if (c.rowStart == 1) chipReq.getValue(profileId) else FocusRequester.Cancel
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
                        ProfileChip(p.name, p.color, p.id == profileId, onFocusLabel, chipNav) {
                            profileId = p.id
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
                CoverPanel(coverBrand, profile.name, isInstalled(coverBrand), Modifier.nav("cover"), onFocusLabel) { launch(coverBrand) }

                watchApps.forEachIndexed { i, b ->
                    WatchPanel(b, i, customIcons, Modifier.nav("w$i"), onFocusLabel) { launch(b) }
                }

                profile.pins.forEachIndexed { i, key ->
                    val b = BRAND_BY_KEY.getValue(key)
                    PinPanel(b, i, profile.name, customIcons, isInstalled(b), Modifier.nav("f$i"), onFocusLabel) { launch(b) }
                }

                NetworkPanel(dns, Modifier.nav("net"), onFocusLabel) {
                    ui.status = "Live NextDNS stats aren't connected yet"
                }
                Panel("Settings — launcher settings arrive in Phase 3", Modifier.nav("settings"), C.surface, onFocusLabel, { ui.status = "Launcher settings arrive in Phase 3" }) {
                    Column(Modifier.fillMaxSize().padding(PAD), verticalArrangement = Arrangement.SpaceBetween) {
                        StrokeIcon(Icons.gear, C.silver, 19.2.dp)
                        Column {
                            T("Settings", 14.4.sp, weight = 700)
                            Spacer(Modifier.height(3.dp))
                            T("Display, icons, profiles", 8.2.sp, color = C.silver)
                        }
                    }
                }
                Panel("Inputs — press OK to choose HDMI or another TV source", Modifier.nav("inputs"), C.surface, onFocusLabel, { ui.status = AppCatalog.openInputPicker(context) }) {
                    Column(Modifier.fillMaxSize().padding(PAD), verticalArrangement = Arrangement.SpaceBetween) {
                        StrokeIcon(Icons.tv, C.silver, 19.2.dp)
                        Column {
                            T("Inputs", 11.5.sp, weight = 700)
                            Spacer(Modifier.height(3.dp))
                            T("HDMI & TV", 7.7.sp, color = C.silver)
                        }
                    }
                }
                Panel("All apps — press OK to see every app on this TV", Modifier.nav("allapps"), C.surface, onFocusLabel, { showAllApps = true }) {
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
            Key("Back"); T("Close", 8.6.sp, color = C.silver)
        }
    }
}

/** A focusable comic panel: cream border, flat fill, yellow ring when selected. */
@Composable
fun Panel(
    label: String, modifier: Modifier, bg: Color, onFocusLabel: (String) -> Unit, onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused, label) { if (focused) onFocusLabel(label) }
    Box(
        modifier
            .focusRing(focused)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .clip(PANEL_SHAPE)
            .background(bg)
            .border(1.5.dp, C.cream, PANEL_SHAPE),
        content = content,
    )
}

@Composable
private fun CoverPanel(brand: Brand, profileName: String, installed: Boolean, modifier: Modifier, onFocusLabel: (String) -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    // Decoded once at near display size, 16-bit, to keep memory low.
    val art = remember {
        BitmapFactory.decodeResource(
            context.resources, R.drawable.cover_collage,
            BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 },
        ).asImageBitmap()
    }
    val label = if (installed) "$profileName's pick: ${brand.name} — press OK to open" else "$profileName's pick: ${brand.name} isn't installed on this TV"
    Panel(label, modifier, C.ink, onFocusLabel, onClick) {
        Image(art, null, Modifier.matchParentSize(), alignment = BiasAlignment(0f, -0.64f), contentScale = ContentScale.Crop)
        Box(
            Modifier.matchParentSize().drawBehind {
                val top = size.height * 0.48f
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.66f to C.ink, 1f to C.ink, startY = top, endY = size.height),
                    topLeft = Offset(0f, top),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height - top),
                )
            },
        )
        Caption("Tonight's issue", 7.7.sp, Modifier.align(Alignment.TopStart).padding(7.7.dp))
        Burst("Play!", 53.8.dp, 9.6.sp, Modifier.align(Alignment.TopEnd).padding(4.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(14.4.dp)) {
            Caption("$profileName's pick", 7.2.sp)
            Spacer(Modifier.height(4.dp))
            T(brand.name, 34.6.sp, family = Bangers, weight = 400, tracking = 0.04.em, lineHeight = 32.sp)
            Spacer(Modifier.height(4.dp))
            T(if (installed) "Press OK to open ${brand.name}" else "${brand.name} isn't installed", 10.1.sp, color = C.silver, weight = 600)
        }
    }
}

@Composable
private fun WatchPanel(b: Brand, slot: Int, custom: Boolean, modifier: Modifier, onFocusLabel: (String) -> Unit, onClick: () -> Unit) {
    val style = BRAND_STYLES.getValue(b.key)
    val labelColor = if (custom) C.text else style.label
    Panel("${b.name} — press OK to open", modifier, if (custom) C.suit else style.bg, onFocusLabel, onClick) {
        if (custom) WebCorner(C.webOnTile, Modifier.align(Alignment.TopEnd).fillMaxHeight(0.3f).heightIn(max = 42.dp).aspectRatio(1f))
        if (slot in SHORT_SLOTS) {
            Row(Modifier.fillMaxSize().padding(horizontal = PAD), verticalAlignment = Alignment.CenterVertically) {
                T(b.name, 12.5.sp, Modifier.weight(1f), color = labelColor, weight = 700)
                Spacer(Modifier.width(6.dp))
                BrandMark(b.key, custom, Modifier.fillMaxHeight(0.5f).aspectRatio(160f / 90f, matchHeightConstraintsFirst = true))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(PAD)) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BrandMark(b.key, custom, Modifier.fillMaxWidth().fillMaxHeight(if (slot == 2) 0.6f else 0.52f))
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    if (slot == 2) T(b.name, 9.1.sp, Modifier.weight(1f), color = labelColor, weight = 700, maxLines = 2)
                    else T(b.name, 12.5.sp, Modifier.weight(1f), color = labelColor, weight = 700)
                    if (slot == 0) T(b.tag.uppercase(), 6.7.sp, color = if (custom) C.silver else style.label, weight = 600, tracking = 0.12.em)
                }
            }
        }
        if (slot == 0) Caption("Watch", 7.2.sp, Modifier.align(Alignment.TopStart))
    }
}

/** Small app badge used inside Favorites panels. */
@Composable
private fun AppChip(b: Brand, custom: Boolean, height: Dp) {
    val style = BRAND_STYLES.getValue(b.key)
    Box(
        Modifier.height(height).aspectRatio(1.6f).clip(RoundedCornerShape(3.dp)).background(if (custom) C.suit else style.bg),
        contentAlignment = Alignment.Center,
    ) { BrandMark(b.key, custom, Modifier.fillMaxSize(0.7f)) }
}

/** A Favorites panel: an honest app shortcut (no fake titles or progress). */
@Composable
private fun PinPanel(b: Brand, index: Int, profileName: String, custom: Boolean, installed: Boolean, modifier: Modifier, onFocusLabel: (String) -> Unit, onClick: () -> Unit) {
    val label = if (installed) "${b.name} — press OK to open" else "${b.name} isn't installed on this TV"
    val sub = if (installed) b.tag else "Not installed"
    Panel(label, modifier, C.surface, onFocusLabel, onClick) {
        if (index == 0) {
            Box(Modifier.align(Alignment.BottomEnd).fillMaxHeight(0.85f).fillMaxWidth(0.65f).halftone(C.halftonePanel, 4.5.dp, 1.dp))
            Column(Modifier.fillMaxSize().padding(PAD), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppChip(b, custom, 26.dp)
                    Spacer(Modifier.width(6.dp))
                    T("App shortcut", 7.7.sp, color = C.silver, weight = 600)
                }
                Column {
                    T(b.name, 15.4.sp, family = Bangers, weight = 400, tracking = 0.04.em)
                    Spacer(Modifier.height(3.dp))
                    T(sub, 8.2.sp, color = if (installed) C.silver else C.red)
                }
            }
            Caption("$profileName's favorites", 7.2.sp, Modifier.align(Alignment.TopStart))
        } else {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 9.6.dp, vertical = 5.4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.6.dp),
            ) {
                AppChip(b, custom, 26.dp)
                Column(Modifier.weight(1f)) {
                    T(b.name, 12.sp, family = Bangers, weight = 400, tracking = 0.04.em)
                    Spacer(Modifier.height(1.dp))
                    T("App shortcut", 7.7.sp, color = C.silver)
                }
                T(sub, 8.2.sp, color = if (installed) C.silver else C.red, weight = 600)
            }
        }
    }
}

@Composable
private fun NetworkPanel(dns: DnsStatus, modifier: Modifier, onFocusLabel: (String) -> Unit, onClick: () -> Unit) {
    Panel("Home base — network status", modifier, C.surface, onFocusLabel, onClick) {
        Column(Modifier.fillMaxSize().padding(start = PAD, end = PAD, bottom = PAD, top = 25.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StrokeIcon(Icons.shield, C.blue, 13.4.dp, strokeWidth = 1.8f)
                Spacer(Modifier.width(5.dp))
                T(dns.title, 10.6.sp, weight = 700)
                Spacer(Modifier.weight(1f))
                T(
                    "NOT LINKED", 6.2.sp, color = C.silver, weight = 600, tracking = 0.08.em,
                    modifier = Modifier.clip(CircleShape).background(C.navy).padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
            Spacer(Modifier.height(7.dp))
            T("NextDNS stats not connected", 10.sp, weight = 700)
            Spacer(Modifier.height(2.dp))
            T("${dns.detail} Query counts appear once a secure link is set up.", 7.2.sp, color = C.silver, maxLines = 3, lineHeight = 10.sp)
            Spacer(Modifier.weight(1f))
            // Empty chart frame: deliberately no bars with fake values.
            Row(Modifier.fillMaxWidth().height(32.4.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                repeat(12) { Box(Modifier.weight(1f).height(2.dp).background(C.rule)) }
            }
            Spacer(Modifier.height(4.dp))
            Row {
                T("12 h ago", 6.2.sp, color = C.silver)
                Spacer(Modifier.weight(1f))
                T("No data yet", 6.2.sp, color = C.silver)
                Spacer(Modifier.weight(1f))
                T("Now", 6.2.sp, color = C.silver)
            }
        }
        Caption("Home base", 7.2.sp, Modifier.align(Alignment.TopStart))
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
            .clickable(interactionSource = source, indication = null, onClick = onClick)
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
