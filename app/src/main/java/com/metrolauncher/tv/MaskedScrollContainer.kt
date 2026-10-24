package com.metrolauncher.tv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Container that applies a progressive vertical gradient alpha mask to its scrolling content.
 * As tiles scroll upward toward the fixed greeting area, only the portion of each tile
 * that enters the fade boundary (between fadeBottom and fadeTop) dissolves into transparency.
 * Tiles below the fade boundary remain 100% opaque. Entire tiles never disappear abruptly.
 */
class MaskedScrollContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density

    // Greeting area ends near 70dp; fade zone spans from 135dp down to 60dp
    private val fadeTopDp = 60f
    private val fadeBottomDp = 135f

    // Small fading on the bottom edge (compact fade zone right at screen bottom)
    private val fadeBottomEdgeDp = 32f

    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    private var cachedWidth = 0
    private var cachedHeight = 0

    init {
        // Required for custom dispatchDraw composition on ViewGroup
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateMask(w, h)
    }

    private fun updateMask(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        cachedWidth = w
        cachedHeight = h

        val fadeTopPx = (fadeTopDp * density).coerceAtMost(h.toFloat())
        val fadeBottomPx = (fadeBottomDp * density).coerceAtMost(h.toFloat())
        val fadeBottomEdgePx = (fadeBottomEdgeDp * density).coerceAtMost(h.toFloat())
        val bottomFadeStartPx = (h.toFloat() - fadeBottomEdgePx).coerceAtLeast(fadeBottomPx)

        val colors = intArrayOf(
            Color.TRANSPARENT,
            Color.TRANSPARENT,
            Color.BLACK,
            Color.BLACK,
            Color.TRANSPARENT
        )
        val positions = floatArrayOf(
            0f,
            (fadeTopPx / h).coerceIn(0f, 1f),
            (fadeBottomPx / h).coerceIn(0f, 1f),
            (bottomFadeStartPx / h).coerceIn(0f, 1f),
            1f
        )

        maskPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            colors,
            positions,
            Shader.TileMode.CLAMP
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) {
            super.dispatchDraw(canvas)
            return
        }

        // On low-end / Performance mode, skip offscreen saveLayer to eliminate GPU memory and fill-rate overhead
        if (LauncherConfig.getPerformanceMode(context) == PerformanceMode.PERFORMANCE) {
            super.dispatchDraw(canvas)
            return
        }

        if (cachedWidth != width || cachedHeight != height) {
            updateMask(width, height)
        }

        // Hardware-accelerated offscreen layer for per-pixel alpha mask
        val count = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.dispatchDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        canvas.restoreToCount(count)
    }
}
