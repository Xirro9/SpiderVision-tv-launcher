package com.spidervision.tv

import com.spidervision.tv.apps.mostOpened
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageTest {
    private val installed = setOf("netflix", "youtube", "spotify", "hulu")

    @Test
    fun nothingOpenedYet() {
        assertNull(mostOpened(emptyMap(), installed, emptyList()))
    }

    @Test
    fun picksTheMostOpenedInstalledApp() {
        assertEquals("youtube" to 7, mostOpened(mapOf("netflix" to 3, "youtube" to 7, "spotify" to 1), installed, emptyList()))
    }

    @Test
    fun skipsAppsAlreadyPinnedAndUninstalledOnes() {
        val launches = mapOf("youtube" to 9, "gone.app" to 20, "netflix" to 4)
        assertEquals("netflix" to 4, mostOpened(launches, installed, exclude = listOf("youtube")))
    }
}
