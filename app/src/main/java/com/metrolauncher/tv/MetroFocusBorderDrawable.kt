package com.metrolauncher.tv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * Custom Metro focus outline drawable featuring a calm, terminal-cursor-inspired
 * breathing intensity pulse.
 *
 * Performance modes:
 * - PERFORMANCE: Animation completely disabled; crisp, static 100% white outline.
 * - BALANCED: Low-cost gentle alpha oscillation (0.80 -> 1.00) over 1200ms.
 * - SMOOTH: Polished sinusoidal breathing (0.65 -> 1.00) over 1500ms with a soft, subtle glow halo.
 */
class MetroFocusBorderDrawable(
    private val context: Context,
    private var fillColor: Int = Color.TRANSPARENT,
    private var focusedFillColor: Int = fillColor,
    private var strokeColor: Int = Color.WHITE,
    private var glowColor: Int = Color.argb(45, 255, 255, 255),
    private val cornerRadiusDp: Float = 2f,
    private val strokeWidthDp: Float = 3f,
    var drawOutline: Boolean = false
) : Drawable(), ValueAnimator.AnimatorUpdateListener {

    private val density = context.resources.displayMetrics.density
    private val cornerRadius = cornerRadiusDp * density
    private val strokeWidth = strokeWidthDp * density

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@MetroFocusBorderDrawable.strokeWidth
        color = strokeColor
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@MetroFocusBorderDrawable.strokeWidth + (2f * density)
        color = glowColor
    }

    private val boundsRect = RectF()
    private var pulseAnimator: ValueAnimator? = null
    private var currentAlphaFactor = 1.0f
    private var isFocused = false

    fun updateFillColor(newColor: Int) {
        if (fillColor != newColor) {
            fillColor = newColor
            fillPaint.color = newColor
            invalidateSelf()
        }
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(stateSet: IntArray): Boolean {
        var focused = false
        for (state in stateSet) {
            if (state == android.R.attr.state_focused) {
                focused = true
                break
            }
        }

        if (focused != isFocused) {
            isFocused = focused
            if (drawOutline) {
                if (isFocused) {
                    startPulseAnimation()
                } else {
                    stopPulseAnimation()
                }
            }
            invalidateSelf()
            return true
        }
        return super.onStateChange(stateSet)
    }

    private fun startPulseAnimation() {
        val perfMode = LauncherConfig.getPerformanceMode(context)
        val selectorAnim = LauncherConfig.getSelectorAnimation(context)

        if (perfMode == PerformanceMode.PERFORMANCE || 
            !LauncherConfig.isAnimationEnabled(context) || 
            selectorAnim == SelectorAnimation.STATIC) {
            stopPulseAnimation()
            currentAlphaFactor = 1.0f
            return
        }

        pulseAnimator?.cancel()
        val duration = when (selectorAnim) {
            SelectorAnimation.BREATHING_GLOW -> if (perfMode == PerformanceMode.SMOOTH) 1500L else 1200L
            SelectorAnimation.PULSE -> 800L
            SelectorAnimation.STATIC -> 0L
        }
        val minAlpha = when (selectorAnim) {
            SelectorAnimation.BREATHING_GLOW -> if (perfMode == PerformanceMode.SMOOTH) 0.65f else 0.78f
            SelectorAnimation.PULSE -> 0.45f
            SelectorAnimation.STATIC -> 1.0f
        }

        pulseAnimator = ValueAnimator.ofFloat(1.0f, minAlpha).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = if (selectorAnim == SelectorAnimation.BREATHING_GLOW) {
                AccelerateDecelerateInterpolator()
            } else {
                LinearInterpolator()
            }
            addUpdateListener(this@MetroFocusBorderDrawable)
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        currentAlphaFactor = 1.0f
        invalidateSelf()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (!visible) {
            stopPulseAnimation()
        } else if (isFocused) {
            startPulseAnimation()
        }
        return changed
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        currentAlphaFactor = animation.animatedValue as Float
        invalidateSelf()
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val inset = strokeWidth / 2f
        boundsRect.set(
            bounds.left + inset,
            bounds.top + inset,
            bounds.right - inset,
            bounds.bottom - inset
        )
    }

    override fun draw(canvas: Canvas) {
        // 1. Draw tile body background
        val currentFill = if (isFocused) focusedFillColor else fillColor
        fillPaint.color = currentFill
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, fillPaint)

        // 2. Draw focus selector outline & glow
        if (isFocused && drawOutline) {
            val perfMode = LauncherConfig.getPerformanceMode(context)
            val selectorAnim = LauncherConfig.getSelectorAnimation(context)

            if (selectorAnim == SelectorAnimation.BREATHING_GLOW && perfMode != PerformanceMode.PERFORMANCE) {
                // Subtle breathing glow halo
                val glowAlpha = (Color.alpha(glowColor) * currentAlphaFactor).toInt().coerceIn(0, 255)
                glowPaint.color = Color.argb(glowAlpha, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor))
                canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, glowPaint)
            }

            // Outline with selected animation intensity
            val strokeAlpha = if (selectorAnim == SelectorAnimation.STATIC || perfMode == PerformanceMode.PERFORMANCE) {
                255
            } else {
                (255 * currentAlphaFactor).toInt().coerceIn(0, 255)
            }
            strokePaint.color = Color.argb(strokeAlpha, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor))
            canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, strokePaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        strokePaint.alpha = alpha
        glowPaint.alpha = (alpha * 0.2f).toInt()
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
        glowPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
