package com.spidervision.tv.apps

/** Apps the "Tonight's issue" cover rotates through, in order (only installed ones are shown). */
val COVER_ROTATION = listOf("netflix", "youtube", "disney", "prime", "crunchyroll", "max")

/**
 * The installed app this profile has opened most from SpiderVision, ignoring [exclude] (apps
 * already pinned next to it), as (package, count). Null until something has been opened.
 */
fun mostOpened(launches: Map<String, Int>, installed: Set<String>, exclude: Collection<String>): Pair<String, Int>? =
    launches.entries
        .filter { it.value > 0 && it.key in installed && it.key !in exclude }
        .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        ?.toPair()
