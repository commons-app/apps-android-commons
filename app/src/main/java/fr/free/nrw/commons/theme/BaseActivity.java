package fr.free.nrw.commons.theme;

import android.content.res.Configuration;
import android.os.Bundle;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerAppCompatActivity;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.settings.Prefs;
import io.reactivex.disposables.CompositeDisposable;

public abstract class BaseActivity extends CommonsDaggerAppCompatActivity {
    @Inject
    @Named("default_preferences")
    public JsonKvStore defaultKvStore;

    protected CompositeDisposable compositeDisposable = new CompositeDisposable();
    protected boolean wasPreviouslyDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wasPreviouslyDarkTheme = getSystemDefaultThemeBool(
                defaultKvStore.getString(Prefs.KEY_THEME_VALUE, getSystemDefaultTheme()));
        setTheme(wasPreviouslyDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);
    }

    @Override
    protected void onResume() {
        // Restart activity if theme is changed
        if (wasPreviouslyDarkTheme != getSystemDefaultThemeBool(
                defaultKvStore.getString(Prefs.KEY_THEME_VALUE, getSystemDefaultTheme()))) {
            recreate();
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    // Return true is system wide dark theme is enabled else false
    private boolean getSystemDefaultThemeBool(String theme) {
        switch (theme) {
            case "Dark":
                return true;
            case "Default":
                return getSystemDefaultThemeBool(getSystemDefaultTheme());
            default:
                return false;
        }
    }

    // Returns the default system wide theme
    private String getSystemDefaultTheme() {
        if ((getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            return "Dark";
        }
        return "Light";
    }

}
