package com.spidervision.tv.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** A brand the design has a custom mark for. Package IDs were confirmed on the target TV. */
data class Brand(val key: String, val name: String, val tag: String, val pkg: String)

val BRANDS = listOf(
    Brand("netflix", "Netflix", "Series & films", "com.netflix.ninja"),
    Brand("youtube", "YouTube", "Subscriptions", "com.google.android.youtube.tv"),
    Brand("prime", "Prime Video", "Movies & sport", "com.amazon.amazonvideo.livingroom"),
    Brand("disney", "Disney+", "Family", "com.disney.disneyplus"),
    Brand("max", "Max", "HBO & more", "com.wbd.stream"),
    Brand("crunchyroll", "Crunchyroll", "Anime", "com.crunchyroll.crunchyroid"),
    Brand("spotify", "Spotify", "Music", "com.spotify.tv.android"),
    Brand("ytmusic", "YouTube Music", "Music", "com.google.android.youtube.tvmusic"),
    Brand("hulu", "Hulu", "Next-day TV", "com.hulu.livingroomplus"),
    Brand("appletv", "Apple TV", "Originals", "com.apple.atve.androidtv.appletv"),
    Brand("peacock", "Peacock", "Live & on demand", "com.peacocktv.peacockandroid"),
    Brand("googletv", "Google TV", "Movies & TV", "com.google.android.videos"),
    Brand("tubi", "Tubi", "Free movies", "com.tubitv"),
    Brand("pluto", "Pluto TV", "Free channels", "tv.pluto.android"),
    Brand("roku", "The Roku Channel", "Free TV", "com.roku.web.trc"),
    Brand("instagram", "Instagram", "Reels", "com.instagram.airwave"),
    Brand("applemusic", "Apple Music", "Music", "com.brkchen.music"),
    Brand("aircast", "AirCast", "Cast from phone", "com.smartdevice.tv.aircast"),
)
val BRAND_BY_KEY = BRANDS.associateBy { it.key }
val BRAND_BY_PKG = BRANDS.associateBy { it.pkg }

/** An app the TV actually exposes in its TV app list right now. */
data class InstalledApp(val pkg: String, val activity: String, val label: String)

object AppCatalog {
    /** Enabled apps with a TV (leanback) launcher entry, excluding SpiderVision itself. */
    fun loadInstalled(context: Context): Map<String, InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .asSequence()
            .map { it.activityInfo }
            .filter { it.enabled && it.applicationInfo.enabled && it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .associate { it.packageName to InstalledApp(it.packageName, it.name, it.loadLabel(pm).toString()) }
    }

    /**
     * The app's own TV banner (or icon if it has none), rendered once at tile size so a
     * 4K-sized drawable is never kept in memory. Call off the main thread.
     */
    fun loadArtwork(context: Context, app: InstalledApp, widthPx: Int, heightPx: Int): Pair<ImageBitmap, Boolean>? {
        val pm = context.packageManager
        val component = ComponentName(app.pkg, app.activity)
        val banner = runCatching { pm.getActivityBanner(component) ?: pm.getApplicationBanner(app.pkg) }.getOrNull()
        val drawable = banner ?: runCatching { pm.getActivityIcon(component) }.getOrNull() ?: return null
        val isBanner = banner != null
        val w = if (isBanner) widthPx else heightPx
        val bmp = Bitmap.createBitmap(w, heightPx, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, w, heightPx)
        drawable.draw(Canvas(bmp))
        return bmp.asImageBitmap() to isBanner
    }

    /**
     * Opens an app through the system's own launch intent (TV entry first, then the regular
     * launcher entry). Returns null on success, or a user-facing reason it could not open.
     */
    fun launch(context: Context, pkg: String, name: String): String? {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
            ?: return "$name isn't installed or can't be opened"
        return start(context, intent, name)
    }

    /** The TV's own "Select input" screen (Google TV input picker on this Philips). */
    fun openInputPicker(context: Context): String? {
        val intent = Intent("com.android.tv.action.VIEW_INPUTS")
        if (intent.resolveActivity(context.packageManager) == null) return "This TV has no input picker SpiderVision can open"
        return start(context, intent, "The input picker")
    }

    private fun start(context: Context, intent: Intent, name: String): String? = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (e: ActivityNotFoundException) {
        "$name couldn't be opened"
    } catch (e: SecurityException) {
        "$name couldn't be opened"
    }
}
