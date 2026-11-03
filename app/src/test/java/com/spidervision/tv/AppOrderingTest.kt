package com.spidervision.tv

import com.spidervision.tv.apps.InstalledApp
import com.spidervision.tv.apps.Move
import com.spidervision.tv.apps.groupPosition
import com.spidervision.tv.apps.moveApp
import com.spidervision.tv.apps.orderedApps
import com.spidervision.tv.profiles.ProfilePrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppOrderingTest {
    private fun app(pkg: String, label: String = pkg) = InstalledApp(pkg, "$pkg.Main", label)

    private val netflix = app("com.netflix.ninja", "Netflix")
    private val youtube = app("com.google.android.youtube.tv", "YouTube")
    private val max = app("com.wbd.stream", "Max")
    private val spotify = app("com.spotify.tv.android", "Spotify")
    private val settings = app("com.android.tv.settings", "Settings")
    private val store = app("com.android.vending", "Play Store")
    private val installed = listOf(netflix, youtube, max, spotify, settings, store).associateBy { it.pkg }

    private fun prefs(favorites: List<String>, order: List<String>) =
        ProfilePrefs(favorites, order, customIcons = true, largeText = false, cover = netflix.pkg, pins = List(4) { netflix.pkg })

    @Test
    fun favouritesLeadThenSavedOrderThenBrandsThenLabels() {
        val p = prefs(favorites = listOf(spotify.pkg), order = listOf(max.pkg, netflix.pkg))
        val result = orderedApps(installed, p).map { it.label }
        // Spotify (favourite) first; Max, Netflix as arranged; YouTube (a known brand) next;
        // then non-brand apps alphabetically.
        assertEquals(listOf("Spotify", "Max", "Netflix", "YouTube", "Play Store", "Settings"), result)
    }

    @Test
    fun uninstalledAppsInSavedOrderAreSkipped() {
        val p = prefs(favorites = listOf("gone.app"), order = listOf("gone.app", netflix.pkg))
        val result = orderedApps(installed, p)
        assertEquals(installed.size, result.size)
        assertEquals(netflix, result.first())
    }

    @Test
    fun moveStaysInsideItsGroup() {
        val p = prefs(favorites = listOf(netflix.pkg, youtube.pkg), order = listOf(netflix.pkg, youtube.pkg, max.pkg, spotify.pkg))
        val current = orderedApps(installed, p)
        // Max is first of the non-favourites: it can't move earlier into the favourites.
        assertNull(moveApp(current, p.favorites, max.pkg, Move.EARLIER))
        val later = moveApp(current, p.favorites, max.pkg, Move.LATER)!!
        assertEquals(listOf(netflix.pkg, youtube.pkg, spotify.pkg, max.pkg), later.take(4))
        val front = moveApp(current, p.favorites, youtube.pkg, Move.FRONT)!!
        assertEquals(listOf(youtube.pkg, netflix.pkg), front.take(2))
    }

    @Test
    fun groupPositionCountsWithinGroup() {
        val p = prefs(favorites = listOf(netflix.pkg), order = listOf(netflix.pkg, youtube.pkg, max.pkg))
        val current = orderedApps(installed, p)
        assertEquals(0 to 1, groupPosition(current, p.favorites, netflix.pkg))
        assertEquals(1 to 5, groupPosition(current, p.favorites, max.pkg))
    }
}
