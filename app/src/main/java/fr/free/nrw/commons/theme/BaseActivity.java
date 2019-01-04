package fr.free.nrw.commons.theme;

import android.os.Bundle;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.di.CommonsDaggerAppCompatActivity;

public abstract class BaseActivity extends CommonsDaggerAppCompatActivity {
    protected boolean wasPreviouslyDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        wasPreviouslyDarkTheme = Utils.isDarkTheme(this);
        setTheme(wasPreviouslyDarkTheme ? R.style.DarkAppTheme : R.style.LightAppTheme);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        // Restart activity if theme is changed
        if (wasPreviouslyDarkTheme != Utils.isDarkTheme(this)) {
            recreate();
        }

        super.onResume();
    }
}
