package com.example.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class WindowBounds(val widthDp: Float, val heightDp: Float)

object WindowGeometry {
    private const val MIN_WIDTH_DP = 240f
    private const val MIN_HEIGHT_DP = 180f
    private const val TITLE_VISIBLE_DP = 48f

    /**
     * The persistence key for one desktop size. Bucketed to 8 dp so a fractional Compose
     * measurement (411.4 x 869.2 dp) still finds the layout saved at 411 x 869 dp, while a
     * monitor and a phone remain separate layouts.
     */
    fun layoutKey(bounds: WindowBounds): String {
        fun bucket(value: Float): Int =
            ((value.coerceAtLeast(1f) / 8f).roundToInt() * 8).coerceAtLeast(8)
        return "${bucket(bounds.widthDp)}x${bucket(bounds.heightDp)}"
    }

    fun normalize(window: DesktopWindow, bounds: WindowBounds): DesktopWindow {
        val maxWidth = max(1f, bounds.widthDp)
        val maxHeight = max(1f, bounds.heightDp)
        val width = min(max(window.width, minWidth(bounds.widthDp)), maxWidth)
        val height = min(max(window.height, minHeight(bounds.heightDp)), maxHeight)
        val maxX = max(0f, bounds.widthDp - width)
        val maxY = max(0f, bounds.heightDp - TITLE_VISIBLE_DP)
        val maxVisibleY = max(0f, bounds.heightDp - height)
        val positionY = window.posY.coerceIn(0f, maxY).coerceAtMost(maxVisibleY)
        return window.copy(
            width = width,
            height = height,
            posX = window.posX.coerceIn(0f, maxX),
            posY = positionY
        )
    }

    fun move(window: DesktopWindow, dxDp: Float, dyDp: Float, bounds: WindowBounds): DesktopWindow =
        normalize(window.copy(posX = window.posX + dxDp, posY = window.posY + dyDp), bounds)

    fun resize(window: DesktopWindow, dWidth: Float, dHeight: Float, bounds: WindowBounds): DesktopWindow {
        val width = (window.width + dWidth).coerceIn(minWidth(bounds.widthDp), max(minWidth(bounds.widthDp), bounds.widthDp))
        val height = (window.height + dHeight).coerceIn(minHeight(bounds.heightDp), max(minHeight(bounds.heightDp), bounds.heightDp))
        return normalize(window.copy(width = width, height = height), bounds)
    }

    private fun minWidth(boundsWidth: Float): Float = min(MIN_WIDTH_DP, max(1f, boundsWidth))
    private fun minHeight(boundsHeight: Float): Float = min(MIN_HEIGHT_DP, max(1f, boundsHeight))
}
