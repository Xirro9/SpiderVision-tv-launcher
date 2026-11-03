package com.spidervision.tv.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.spidervision.tv.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * Owner-supplied comic art (art-src/, cropped by tools/make_art.ps1 to the exact size each tile is
 * shown at). Decoded off the main thread as 16-bit bitmaps and kept in a small LRU cache.
 */

/** Tile art per designed brand (400×300). */
val APP_ART: Map<String, Int> = mapOf(
    "netflix" to R.drawable.art_netflix, "youtube" to R.drawable.art_youtube, "prime" to R.drawable.art_prime,
    "disney" to R.drawable.art_disney, "max" to R.drawable.art_max, "crunchyroll" to R.drawable.art_crunchyroll,
    "spotify" to R.drawable.art_spotify, "ytmusic" to R.drawable.art_ytmusic, "hulu" to R.drawable.art_hulu,
    "appletv" to R.drawable.art_appletv, "peacock" to R.drawable.art_peacock, "googletv" to R.drawable.art_googletv,
    "tubi" to R.drawable.art_tubi, "pluto" to R.drawable.art_pluto, "roku" to R.drawable.art_roku,
    "instagram" to R.drawable.art_instagram, "applemusic" to R.drawable.art_applemusic, "aircast" to R.drawable.art_aircast,
)

/** Portrait cover art for the rotating "Tonight's issue" (600×860). */
val COVER_ART: Map<String, Int> = mapOf(
    "netflix" to R.drawable.cover_netflix, "youtube" to R.drawable.cover_youtube, "disney" to R.drawable.cover_disney,
    "prime" to R.drawable.cover_prime, "crunchyroll" to R.drawable.cover_crunchyroll, "max" to R.drawable.cover_max,
)

object ArtImages {
    // ~8 MB of 16-bit pixels: every tile and panel (~5.5 MB) plus two covers during a crossfade.
    private val cache = object : LruCache<Int, ImageBitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: Int, value: ImageBitmap) = value.width * value.height * 2
    }

    fun cached(res: Int): ImageBitmap? = cache.get(res)

    suspend fun load(context: Context, res: Int): ImageBitmap = withContext(Dispatchers.IO) {
        cache.get(res) ?: BitmapFactory.decodeResource(
            context.resources, res,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.RGB_565 },
        ).asImageBitmap().also { cache.put(res, it) }
    }
}

@Composable
fun rememberArt(res: Int): ImageBitmap? {
    val context = LocalContext.current
    val art by produceState(ArtImages.cached(res), res) { if (value == null) value = ArtImages.load(context, res) }
    return art
}

/** 0 → 1 while the enclosing panel is focused; read at draw time so it never recomposes. */
val LocalPanelLift = compositionLocalOf<() -> Float> { { 0f } }

/**
 * Art filling its box. [zoom] > 0 makes it zoom in as its panel gains focus; off by default because
 * it forces the panel's cached art to be redrawn on every frame of the lift (measured: 19 → 24 ms).
 */
@Composable
fun ArtBackground(res: Int, modifier: Modifier, alignment: Alignment = Alignment.Center, zoom: Float = 0f) {
    val art = rememberArt(res) ?: return
    val lift = LocalPanelLift.current
    Image(
        art, null,
        if (zoom == 0f) modifier else modifier.graphicsLayer { val s = 1f + zoom * lift(); scaleX = s; scaleY = s },
        alignment = alignment, contentScale = ContentScale.Crop,
    )
}

/** Darkens art from top to bottom so logos and names stay readable. */
val TILE_SCRIM = Brush.verticalGradient(
    0f to C.suit.copy(alpha = 0.18f), 0.5f to C.suit.copy(alpha = 0.5f), 1f to C.suit.copy(alpha = 0.93f),
)

/** Heavier, even darkening for panels that carry text over art. */
fun panelScrim(top: Float, bottom: Float, color: Color = C.surface) =
    Brush.verticalGradient(0f to color.copy(alpha = top), 1f to color.copy(alpha = bottom))
