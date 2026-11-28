package fr.free.nrw.commons.ui.widget

import android.animation.ValueAnimator
import android.content.res.Resources
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import fr.free.nrw.commons.R
import org.xmlpull.v1.XmlPullParser

/**
 * Loading circle that shows a still ring when animations are off (for example in Battery Saver),
 * instead of Android's "refresh" arrow. When animations are on, it is Android's normal spinner.
 */
@RequiresApi(Build.VERSION_CODES.O)
class LoadingCircleDrawable :
    DrawableWrapper(null),
    Animatable {
    private var spinner: Drawable? = null
    private var stillRing: Drawable? = null
    private var resources: Resources? = null
    private var spinnerStyle = android.R.style.Widget_Material_ProgressBar

    override fun inflate(
        r: Resources,
        parser: XmlPullParser,
        attrs: AttributeSet,
        theme: Resources.Theme?,
    ) {
        val a = r.obtainAttributes(attrs, R.styleable.LoadingCircleDrawable)
        spinnerStyle = a.getResourceId(R.styleable.LoadingCircleDrawable_loadingSpinnerStyle, spinnerStyle)
        a.recycle()
        resources = r
        theme?.let { applyTheme(it) }
    }

    // Both pictures take their colour from the theme, so they are made once the theme is known
    override fun canApplyTheme(): Boolean = true

    override fun applyTheme(t: Resources.Theme) {
        val r = resources ?: return

        val spinnerAttrs = t.obtainStyledAttributes(spinnerStyle, intArrayOf(android.R.attr.indeterminateDrawable))
        spinner = spinnerAttrs.getDrawable(0)
        spinnerAttrs.recycle()

        stillRing = ResourcesCompat.getDrawable(r, R.drawable.loading_circle_still, t)
        drawable = pickDrawable()
    }

    private fun pickDrawable(): Drawable? = if (ValueAnimator.areAnimatorsEnabled()) spinner else stillRing

    /**
     * True when the still ring is shown, because animations are turned off.
     */
    fun isShowingStillRing(): Boolean = drawable != null && drawable === stillRing

    override fun start() {
        drawable = pickDrawable()
        (drawable as? Animatable)?.start()
    }

    override fun stop() {
        (spinner as? Animatable)?.stop()
    }

    override fun isRunning(): Boolean = (drawable as? Animatable)?.isRunning ?: false

    // Without a constant state, Android creates a new copy from the XML for every loading circle,
    // instead of a plain DrawableWrapper copy that would lose the behaviour above.
    override fun getConstantState(): ConstantState? = null
}
