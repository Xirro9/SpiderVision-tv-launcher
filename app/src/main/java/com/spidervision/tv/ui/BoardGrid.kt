package com.spidervision.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt

/** Grid placement, CSS style: columns/rows are 1-based and the end line is exclusive. */
data class Cell(val colStart: Int, val colEnd: Int, val rowStart: Int, val rowEnd: Int)

private class CellParentData(val cell: Cell) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = cell
}

fun Modifier.cell(colStart: Int, colEnd: Int, rowStart: Int, rowEnd: Int): Modifier =
    this.then(CellParentData(Cell(colStart, colEnd, rowStart, rowEnd)))

/**
 * Fixed comic-page grid: every child is placed by its [cell]. No scrolling or reflow, so the
 * D-pad map is the same on every screen.
 */
@Composable
fun BoardGrid(columns: Int, rows: Int, gap: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val g = gap.toPx()
        val cw = (constraints.maxWidth - g * (columns - 1)) / columns
        val rh = (constraints.maxHeight - g * (rows - 1)) / rows
        val placed = measurables.map { m ->
            val c = m.parentData as Cell
            val w = (c.colEnd - c.colStart) * cw + (c.colEnd - c.colStart - 1) * g
            val h = (c.rowEnd - c.rowStart) * rh + (c.rowEnd - c.rowStart - 1) * g
            Triple(m.measure(Constraints.fixed(w.roundToInt(), h.roundToInt())), (c.colStart - 1) * (cw + g), (c.rowStart - 1) * (rh + g))
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            placed.forEach { (p, x, y) -> p.place(x.roundToInt(), y.roundToInt()) }
        }
    }
}
