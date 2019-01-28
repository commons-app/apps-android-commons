package fr.free.nrw.commons.theme;

import android.os.Bundle;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerAppCompatActivity;
import fr.free.nrw.commons.kvstore.BasicKvStore;

public abstract class BaseActivity extends CommonsDaggerAppCompatActivity {
    @Inject
    @Named("default_preferences")
    BasicKvStore defaultKvStore;

    protected boolean wasPreviouslyDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wasPreviouslyDarkTheme = defaultKvStore.getBoolean("theme", false);
        setTheme(wasPreviouslyDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);
    }

    @Override
    protected void onResume() {
        // Restart activity if theme is changed
        if (wasPreviouslyDarkTheme != defaultKvStore.getBoolean("theme", false)) {
            recreate();
        }

        super.onResume();
    }
}
