package com.spidervision.tv.apps

import com.spidervision.tv.profiles.ProfilePrefs

enum class Move { EARLIER, LATER, FRONT }

/**
 * Installed apps in the profile's order: favourites first, then everything else. Apps the
 * user hasn't arranged yet follow the saved order, designed brands before other apps.
 */
fun orderedApps(installed: Map<String, InstalledApp>, prefs: ProfilePrefs): List<InstalledApp> {
    val arranged = prefs.order.mapNotNull { installed[it] }
    val brandRank = BRANDS.withIndex().associate { (i, b) -> b.pkg to i }
    val rest = installed.values.filter { it.pkg !in prefs.order }
        .sortedWith(compareBy<InstalledApp> { brandRank[it.pkg] ?: Int.MAX_VALUE }.thenBy { it.label.lowercase() })
    val all = arranged + rest
    return all.filter { it.pkg in prefs.favorites } + all.filter { it.pkg !in prefs.favorites }
}

/** Moves [pkg] within its own group (favourites or the rest). Returns the new full order. */
fun moveApp(current: List<InstalledApp>, favorites: List<String>, pkg: String, how: Move): List<String>? {
    val ids = current.map { it.pkg }
    val isFav = pkg in favorites
    val group = ids.filter { (it in favorites) == isFav }.toMutableList()
    val i = group.indexOf(pkg)
    if (i < 0) return null
    val j = when (how) { Move.FRONT -> 0; Move.EARLIER -> i - 1; Move.LATER -> i + 1 }
    if (j !in group.indices || j == i) return null
    group.removeAt(i)
    group.add(j, pkg)
    val other = ids.filter { (it in favorites) != isFav }
    return if (isFav) group + other else other + group
}

/** Position of [pkg] inside its group, as (index, groupSize). */
fun groupPosition(current: List<InstalledApp>, favorites: List<String>, pkg: String): Pair<Int, Int> {
    val isFav = pkg in favorites
    val group = current.filter { (it.pkg in favorites) == isFav }
    return group.indexOfFirst { it.pkg == pkg } to group.size
}
