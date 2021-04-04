package fr.free.nrw.commons.theme;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import javax.inject.Inject;
import javax.inject.Named;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerAppCompatActivity;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseActivity extends CommonsDaggerAppCompatActivity {
    @Inject
    @Named("default_preferences")
    public JsonKvStore defaultKvStore;

    @Inject
    SystemThemeUtils systemThemeUtils;

    protected CompositeDisposable compositeDisposable = new CompositeDisposable();
    protected boolean wasPreviouslyDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wasPreviouslyDarkTheme = systemThemeUtils.isDeviceInNightMode();
        setTheme(wasPreviouslyDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);
        float fontScale = android.provider.Settings.System.getFloat(
            getBaseContext().getContentResolver(),
            android.provider.Settings.System.FONT_SCALE,
            1f);
        adjustFontScale(getResources().getConfiguration(), fontScale);
    }

    @Override
    protected void onResume() {
        // Restart activity if theme is changed
        if (wasPreviouslyDarkTheme != systemThemeUtils.isDeviceInNightMode()) {
            recreate();
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    /**
     * Apply fontScale on device
     */
    public void adjustFontScale(Configuration configuration, float scale) {
        configuration.fontScale = scale;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

}
