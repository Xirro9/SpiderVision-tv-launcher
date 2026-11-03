package com.spidervision.tv.profiles

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.spidervision.tv.apps.BRAND_BY_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("spidervision")

/** One profile's saved choices. Apps are stored by package name, so any installed app works. */
data class ProfilePrefs(
    val favorites: List<String>,
    /** Full app order the user has arranged; apps not listed follow after it. */
    val order: List<String>,
    val customIcons: Boolean,
    val largeText: Boolean,
    val cover: String,
    /** Exactly four pinned apps for the Favorites panels. */
    val pins: List<String>,
)

data class LauncherPrefs(val active: String, val profiles: Map<String, ProfilePrefs>) {
    val current: ProfilePrefs get() = profiles.getValue(active)
}

private val ACTIVE = stringPreferencesKey("active_profile")
private fun k(profile: String, name: String) = stringPreferencesKey("$profile.$name")
private fun b(profile: String, name: String) = booleanPreferencesKey("$profile.$name")
private fun pkgOf(brandKey: String) = BRAND_BY_KEY.getValue(brandKey).pkg
private fun String.toList() = split(',').filter { it.isNotBlank() }

/**
 * Saved launcher state (Preferences DataStore, app-private storage). Survives force-stop,
 * reboot and app updates; cleared only by uninstalling or "Reset" in Settings.
 * No account data, credentials or browsing data is stored here.
 */
class LauncherStore(private val context: Context) {

    val prefs: Flow<LauncherPrefs> = context.dataStore.data.map { p ->
        val active = p[ACTIVE]?.takeIf { id -> DEFAULT_PROFILES.any { it.id == id } } ?: DEFAULT_PROFILES.first().id
        LauncherPrefs(active, DEFAULT_PROFILES.associate { it.id to read(p, it) })
    }

    private fun read(p: Preferences, d: Profile) = ProfilePrefs(
        favorites = p[k(d.id, "favorites")]?.toList() ?: DEFAULT_FAVORITE_APPS.map(::pkgOf),
        order = p[k(d.id, "order")]?.toList() ?: DEFAULT_APP_ORDER.map(::pkgOf),
        customIcons = p[b(d.id, "customIcons")] ?: true,
        largeText = p[b(d.id, "largeText")] ?: false,
        cover = p[k(d.id, "cover")] ?: pkgOf(d.coverApp),
        pins = (p[k(d.id, "pins")]?.toList() ?: d.pins.map(::pkgOf)).let { saved ->
            // Always four slots; fall back to the defaults if storage was ever short.
            List(4) { saved.getOrNull(it) ?: pkgOf(d.pins[it]) }
        },
    )

    private fun write(p: MutablePreferences, id: String, v: ProfilePrefs) {
        p[k(id, "favorites")] = v.favorites.joinToString(",")
        p[k(id, "order")] = v.order.joinToString(",")
        p[b(id, "customIcons")] = v.customIcons
        p[b(id, "largeText")] = v.largeText
        p[k(id, "cover")] = v.cover
        p[k(id, "pins")] = v.pins.joinToString(",")
    }

    suspend fun setActive(id: String) {
        context.dataStore.edit { it[ACTIVE] = id }
    }

    suspend fun update(id: String, change: (ProfilePrefs) -> ProfilePrefs) {
        context.dataStore.edit { p ->
            val d = DEFAULT_PROFILES.first { it.id == id }
            write(p, id, change(read(p, d)))
        }
    }

    /** Put one profile back to its defaults. */
    suspend fun reset(id: String) {
        context.dataStore.edit { p ->
            listOf("favorites", "order", "cover", "pins").forEach { p.remove(k(id, it)) }
            listOf("customIcons", "largeText").forEach { p.remove(b(id, it)) }
        }
    }
}
