package com.spidervision.tv.profiles

import androidx.compose.ui.graphics.Color
import com.spidervision.tv.ui.C

/**
 * Launcher-local household profiles. They only change which shortcuts SpiderVision shows;
 * they are not Google accounts and do not restrict anything.
 *
 * Phase 1 keeps these in memory. Persistence (DataStore) arrives in Phase 3.
 */
data class Profile(
    val id: String,
    val name: String,
    val color: Color,
    /** Brand key opened by the cover panel. */
    val coverApp: String,
    /** Brand keys pinned in the Favorites panels, in order. */
    val pins: List<String>,
)

val DEFAULT_PROFILES = listOf(
    Profile("mahin", "Mahin", C.red, "netflix", listOf("spotify", "crunchyroll", "youtube", "ytmusic")),
    Profile("aria", "Aria", C.blue, "prime", listOf("hulu", "max", "instagram", "applemusic")),
    Profile("kids", "Kids", Color(0xFFE9C46A), "disney", listOf("disney", "pluto", "tubi", "spotify")),
    Profile("guest", "Guest", C.silver, "tubi", listOf("youtube", "aircast", "roku", "googletv")),
)

/** Favourite apps lead the Watch panels; the rest follow in this order. */
val DEFAULT_FAVORITE_APPS = listOf("netflix", "youtube", "prime", "disney")
val DEFAULT_APP_ORDER = listOf(
    "netflix", "youtube", "prime", "disney", "max", "crunchyroll", "spotify", "ytmusic", "hulu",
    "appletv", "peacock", "googletv", "tubi", "pluto", "roku", "instagram", "applemusic", "aircast",
)
