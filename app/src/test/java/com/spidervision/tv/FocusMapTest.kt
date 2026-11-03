package com.spidervision.tv

import com.spidervision.tv.ui.Cell
import com.spidervision.tv.ui.computeNeighbors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The home board's D-pad map, using the same cells as HomeScreen. */
class FocusMapTest {
    private val cells = mapOf(
        "cover" to Cell(1, 5, 1, 7),
        "w0" to Cell(5, 7, 1, 3), "w1" to Cell(7, 10, 1, 2), "w2" to Cell(7, 8, 2, 4),
        "w3" to Cell(8, 10, 2, 3), "w4" to Cell(8, 10, 3, 4), "w5" to Cell(5, 7, 3, 4),
        "f0" to Cell(5, 7, 4, 6), "f1" to Cell(7, 10, 4, 5), "f2" to Cell(7, 10, 5, 6), "f3" to Cell(5, 10, 6, 7),
        "net" to Cell(10, 13, 1, 4), "settings" to Cell(10, 12, 4, 6), "inputs" to Cell(12, 13, 4, 6), "allapps" to Cell(10, 13, 6, 7),
    )
    private val n = computeNeighbors(cells)

    @Test
    fun walkMatchesTheVerifiedRemotePath() {
        // Same path verified on the TV in Phase 2.
        assertEquals("w0", n.getValue("cover").right)
        assertEquals("w1", n.getValue("w0").right)
        assertEquals("net", n.getValue("w1").right)
        assertEquals("settings", n.getValue("net").down)
        assertEquals("inputs", n.getValue("settings").right)
        assertEquals("allapps", n.getValue("inputs").down)
        assertEquals("f3", n.getValue("allapps").left)
        assertEquals("f0", n.getValue("f3").up)
        assertEquals("cover", n.getValue("f0").left)
    }

    @Test
    fun edgesGoNowhere() {
        assertNull(n.getValue("cover").left)
        assertNull(n.getValue("cover").down)
        assertNull(n.getValue("net").right)
        assertNull(n.getValue("allapps").down)
        assertNull(n.getValue("w0").up) // HomeScreen sends this to the active profile chip
    }

    @Test
    fun everyPanelIsReachableFromTheCover() {
        val seen = mutableSetOf("cover")
        val queue = ArrayDeque(listOf("cover"))
        while (queue.isNotEmpty()) {
            val nb = n.getValue(queue.removeFirst())
            listOfNotNull(nb.left, nb.right, nb.up, nb.down).filter { seen.add(it) }.forEach(queue::addLast)
        }
        assertEquals(cells.keys, seen)
    }

    @Test
    fun noPanelHasADeadEnd() {
        // Every panel can be left in at least one direction (no remote trap).
        cells.keys.forEach { id ->
            val nb = n.getValue(id)
            assertTrue(id, listOfNotNull(nb.left, nb.right, nb.up, nb.down).isNotEmpty())
        }
    }
}
