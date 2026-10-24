package com.metrolauncher.tv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * Top-level focus selector overlay view drawn above the header gradient scrim.
 * This guarantees the focus selector outline & breathing glow remain 100% visible,
 * crisp, and completely unaffected by the top fade effect as tiles glide underneath.
 */
class FocusSelectorOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    private val density = context.resources.displayMetrics.density
    private val strokeWidth = 3f * density
    private val cornerRadius = 2f * density

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@FocusSelectorOverlayView.strokeWidth
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@FocusSelectorOverlayView.strokeWidth + (2f * density)
    }

    private val boundsRect = RectF()
    private var pulseAnimator: ValueAnimator? = null
    private var currentAlphaFactor = 1.0f

    var currentFocusedView: View? = null
        private set

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (currentFocusedView != null && isShown) {
            invalidate()
        }
        true
    }

    fun bindFocusedView(view: View?) {
        if (currentFocusedView === view) return

        currentFocusedView = view
        if (view != null && view.isAttachedToWindow && view.isShown) {
            startPulseAnimation()
            visibility = View.VISIBLE
            viewTreeObserver.removeOnPreDrawListener(preDrawListener)
            viewTreeObserver.addOnPreDrawListener(preDrawListener)
        } else {
            stopPulseAnimation()
            visibility = View.GONE
            viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        }
        invalidate()
    }

    fun refreshAnimationState() {
        if (currentFocusedView != null) {
            startPulseAnimation()
            invalidate()
        }
    }

    private fun startPulseAnimation() {
        val perfMode = LauncherConfig.getPerformanceMode(context)
        val selectorAnim = LauncherConfig.getSelectorAnimation(context)

        pulseAnimator?.cancel()
        if (perfMode == PerformanceMode.PERFORMANCE ||
            !LauncherConfig.isAnimationEnabled(context) ||
            selectorAnim == SelectorAnimation.STATIC) {
            currentAlphaFactor = 1.0f
            return
        }

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
            addUpdateListener(this@FocusSelectorOverlayView)
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        currentAlphaFactor = 1.0f
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        currentAlphaFactor = animation.animatedValue as Float
        invalidate()
    }

    private fun getLayoutLocationInWindow(view: View, outLoc: IntArray) {
        outLoc[0] = 0
        outLoc[1] = 0
        var current: View? = view
        while (current != null && current !== current.rootView) {
            outLoc[0] += current.left - current.scrollX
            outLoc[1] += current.top - current.scrollY
            val parent = current.parent
            if (parent is View) {
                current = parent
            } else {
                break
            }
        }
    }

    private fun getEffectiveAlpha(view: View): Float {
        var alpha = view.alpha
        var current: View? = view
        while (current != null && current !== current.rootView) {
            val parent = current.parent
            if (parent is View) {
                alpha *= parent.alpha
                current = parent
            } else {
                break
            }
        }
        return alpha.coerceIn(0f, 1f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val target = currentFocusedView ?: return
        if (!target.isAttachedToWindow || !target.isShown || target.width == 0 || target.height == 0) return

        val effectiveAlpha = getEffectiveAlpha(target)
        if (effectiveAlpha <= 0.01f) return

        val targetLoc = IntArray(2)
        getLayoutLocationInWindow(target, targetLoc)
        val overlayLoc = IntArray(2)
        getLayoutLocationInWindow(this, overlayLoc)

        val left = (targetLoc[0] - overlayLoc[0]).toFloat()
        val top = (targetLoc[1] - overlayLoc[1]).toFloat()
        val width = target.width.toFloat()
        val height = target.height.toFloat()

        val inset = strokeWidth / 2f
        boundsRect.set(inset, inset, width - inset, height - inset)

        val theme = LauncherConfig.getActiveTileTheme(context)
        val strokeColor = theme.focusStrokeColor
        val glowColor = theme.focusGlowColor
        val selectorAnim = LauncherConfig.getSelectorAnimation(context)
        val perfMode = LauncherConfig.getPerformanceMode(context)

        canvas.save()
        // Clip so selector does not bleed into greeting area
        val clipTop = 60f * density
        canvas.clipRect(0f, clipTop, this.width.toFloat(), this.height.toFloat())
        // Firmly track untransformed layout position so 3D flip/scale animations inside tile do not wiggle the selector
        canvas.translate(left, top)

        if (selectorAnim == SelectorAnimation.BREATHING_GLOW && perfMode != PerformanceMode.PERFORMANCE) {
            val glowAlpha = (Color.alpha(glowColor) * currentAlphaFactor * effectiveAlpha).toInt().coerceIn(0, 255)
            glowPaint.color = Color.argb(glowAlpha, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor))
            canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, glowPaint)
        }

        val baseStrokeAlpha = if (selectorAnim == SelectorAnimation.STATIC || perfMode == PerformanceMode.PERFORMANCE) {
            255
        } else {
            (255 * currentAlphaFactor).toInt().coerceIn(0, 255)
        }
        val strokeAlpha = (baseStrokeAlpha * effectiveAlpha).toInt().coerceIn(0, 255)
        strokePaint.color = Color.argb(strokeAlpha, Color.red(strokeColor), Color.green(strokeColor), Color.blue(strokeColor))
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, strokePaint)

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulseAnimation()
        viewTreeObserver.removeOnPreDrawListener(preDrawListener)
    }
}
