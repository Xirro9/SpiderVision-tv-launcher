package com.spidervision.tv.ui

/**
 * Deterministic D-pad map for the comic board, computed from each panel's grid cell.
 *
 * Rule, so every press is predictable:
 *  - Left/Right go to the panel touching this one's side at this panel's TOP row.
 *  - Up/Down go to the panel touching this one's edge at this panel's LEFT column.
 *  - No panel there → the press does nothing (except Up from the top row, which goes to the
 *    active profile chip).
 */
data class Neighbors(val left: String?, val right: String?, val up: String?, val down: String?)

fun computeNeighbors(cells: Map<String, Cell>): Map<String, Neighbors> = cells.mapValues { (id, c) ->
    fun find(pred: (Cell) -> Boolean) = cells.entries.firstOrNull { (other, oc) -> other != id && pred(oc) }?.key
    Neighbors(
        left = find { it.colEnd == c.colStart && c.rowStart in it.rowStart until it.rowEnd },
        right = find { it.colStart == c.colEnd && c.rowStart in it.rowStart until it.rowEnd },
        up = find { it.rowEnd == c.rowStart && c.colStart in it.colStart until it.colEnd },
        down = find { it.rowStart == c.rowEnd && c.colStart in it.colStart until it.colEnd },
    )
}
