package com.spidervision.tv.ui

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import com.spidervision.tv.apps.AppCatalog
import com.spidervision.tv.apps.BRAND_BY_PKG
import com.spidervision.tv.apps.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An app's own TV banner (true) or icon (false), rendered once at a small fixed size. */
data class AppArt(val bitmap: ImageBitmap, val isBanner: Boolean)

/**
 * Artwork for apps without a designed brand mark. Each bitmap is rendered once at tile size
 * (~200×112 px, ~90 KB) and kept for the life of the screen; brand apps never load artwork.
 */
class ArtCache {
    val art = mutableStateMapOf<String, AppArt>()

    suspend fun ensure(context: Context, apps: List<InstalledApp>, heightPx: Int) {
        val widthPx = heightPx * 16 / 9
        for (app in apps) {
            if (BRAND_BY_PKG.containsKey(app.pkg) || art.containsKey(app.pkg)) continue
            val loaded = withContext(Dispatchers.IO) { AppCatalog.loadArtwork(context, app, widthPx, heightPx) }
            if (loaded != null) art[app.pkg] = AppArt(loaded.first, loaded.second)
        }
    }
}
