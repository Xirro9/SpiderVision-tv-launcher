package com.spidervision.tv.profiles

import androidx.compose.ui.graphics.Color
import com.spidervision.tv.ui.C

/**
 * SpiderVision profiles, named after the household's Google TV profiles. Each keeps its own
 * saved layout. They are not the Google accounts themselves: Android does not let other apps
 * read or switch Google TV profiles, so switching here changes only SpiderVision's layout.
 */
data class Profile(
    val id: String,
    val name: String,
    val color: Color,
    /** Brand key opened by the cover panel, until the person picks their own. */
    val coverApp: String,
    /** Brand keys pinned in the Favorites panels, until the person pins their own. */
    val pins: List<String>,
)

val DEFAULT_PROFILES = listOf(
    Profile("mahin", "Mahin", C.red, "netflix", listOf("spotify", "crunchyroll", "youtube", "ytmusic")),
    Profile("tabitha", "Tabitha", C.blue, "prime", listOf("hulu", "ytmusic", "aircast", "applemusic")),
    Profile("john", "John", Color(0xFFE9C46A), "max", listOf("youtube", "tubi", "pluto", "spotify")),
    Profile("raph", "Raph", C.silver, "youtube", listOf("crunchyroll", "ytmusic", "roku", "aircast")),
)

/** Favourite apps lead the Watch panels; the rest follow in this order. */
val DEFAULT_FAVORITE_APPS = listOf("netflix", "youtube", "prime", "disney")
val DEFAULT_APP_ORDER = listOf(
    "netflix", "youtube", "prime", "disney", "max", "crunchyroll", "spotify", "ytmusic", "hulu",
    "appletv", "peacock", "googletv", "tubi", "pluto", "roku", "instagram", "applemusic", "aircast",
)
