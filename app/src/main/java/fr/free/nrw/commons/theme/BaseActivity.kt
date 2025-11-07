package fr.free.nrw.commons.theme

import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Named
import fr.free.nrw.commons.R
import fr.free.nrw.commons.di.CommonsDaggerAppCompatActivity
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.utils.SystemThemeUtils
import io.reactivex.disposables.CompositeDisposable

@AndroidEntryPoint
abstract class BaseActivity : CommonsDaggerAppCompatActivity() {

    @Inject
    @field:Named("default_preferences")
    lateinit var defaultKvStore: JsonKvStore

    @Inject
    lateinit var systemThemeUtils: SystemThemeUtils

    protected val compositeDisposable = CompositeDisposable()
    protected var wasPreviouslyDarkTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wasPreviouslyDarkTheme = systemThemeUtils.isDeviceInNightMode()
        setTheme(if (wasPreviouslyDarkTheme) R.style.DarkAppTheme else R.style.LightAppTheme)

        val fontScale = android.provider.Settings.System.getFloat(
            baseContext.contentResolver,
            android.provider.Settings.System.FONT_SCALE,
            1f
        )
        adjustFontScale(resources.configuration, fontScale)
        enableEdgeToEdge()
    }

    override fun onResume() {
        // Restart activity if theme is changed
        if (wasPreviouslyDarkTheme != systemThemeUtils.isDeviceInNightMode()) {
            recreate()
        }
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    /**
     * Apply fontScale on device
     */
    fun adjustFontScale(configuration: Configuration, scale: Float) {
        configuration.fontScale = scale
        val metrics = resources.displayMetrics
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        metrics.scaledDensity = configuration.fontScale * metrics.density
        baseContext.resources.updateConfiguration(configuration, metrics)
    }
}
