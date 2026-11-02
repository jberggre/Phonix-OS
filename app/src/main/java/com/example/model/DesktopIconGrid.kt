package com.example.model

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The desktop icon grid, defined once.
 *
 * The renderer and the view model used to keep two: the desktop drew icons bottom-left on a
 * 90 x 88 dp pitch, while "Lock Icons to Grid" snapped with its own 88 x 94 dp pitch measured from
 * the top corner and clamped rows at 7. Locking therefore moved a dragged icon to the middle of the
 * screen and left every un-dragged icon untouched, because only icons with a custom position were
 * visited at all.
 *
 * Rows are counted from the **bottom** of the icon area, because that is where desktop icons live:
 * row 0 is the bottom row, the one the default layout draws into.
 */
object DesktopIconGrid {
    const val COLUMN_PITCH_DP = 90f
    const val ROW_PITCH_DP = 88f

    /** Gap kept under the bottom row, inside the icon layer. */
    const val BOTTOM_GAP_DP = 6f

    fun columns(availableWidthDp: Float): Int =
        (availableWidthDp / COLUMN_PITCH_DP).toInt().coerceAtLeast(1)

    fun rows(count: Int, columns: Int): Int =
        ((count + columns - 1) / columns).coerceAtLeast(1)

    /** Top of the bottom row: the lowest an icon may sit and still be inside the area. */
    fun bottomRowTop(areaHeightDp: Float): Float =
        max(0f, areaHeightDp - BOTTOM_GAP_DP - ROW_PITCH_DP)

    /** Highest row index that still fits above the bottom edge. */
    fun maxRow(areaHeightDp: Float): Int =
        max(1, ((areaHeightDp - BOTTOM_GAP_DP) / ROW_PITCH_DP).toInt()) - 1

    /** Top-left of the slot [row] rows above the bottom edge of the icon area. */
    fun snap(column: Int, row: Int, areaHeightDp: Float): Pair<Float, Float> {
        val up = row.coerceAtLeast(0)
        return Pair(
            column.coerceAtLeast(0) * COLUMN_PITCH_DP,
            bottomRowTop(areaHeightDp) - up * ROW_PITCH_DP
        )
    }

    /**
     * Where an icon that has never been dragged is drawn: the block of rows is anchored to the
     * bottom of the icon area, which keeps the wallpaper clear above it.
     */
    fun autoPosition(index: Int, count: Int, columns: Int, areaHeightDp: Float): Pair<Float, Float> {
        val cols = columns.coerceAtLeast(1)
        val safeIndex = index.coerceAtLeast(0)
        val rowFromTop = safeIndex / cols
        val totalRows = rows(count, cols)
        return Pair(
            (safeIndex % cols) * COLUMN_PITCH_DP,
            bottomRowTop(areaHeightDp) - (totalRows - 1 - rowFromTop) * ROW_PITCH_DP
        )
    }

    fun nearestColumn(posX: Float, columns: Int): Int =
        (posX / COLUMN_PITCH_DP).roundToInt().coerceIn(0, (columns - 1).coerceAtLeast(0))

    fun nearestRow(posY: Float, areaHeightDp: Float): Int =
        ((bottomRowTop(areaHeightDp) - posY) / ROW_PITCH_DP).roundToInt()
            .coerceIn(0, maxRow(areaHeightDp))
}
