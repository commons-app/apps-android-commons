package fr.free.nrw.commons.theme;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import dagger.android.support.DaggerAppCompatActivity;
import fr.free.nrw.commons.R;

public abstract class BaseActivity extends DaggerAppCompatActivity {
    boolean currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean currentThemeIsDark = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme", false);
        if (currentThemeIsDark) {
            currentTheme = true;
            setTheme(R.style.DarkAppTheme);
        } else {
            currentTheme = false;
            setTheme(R.style.LightAppTheme); // default
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        // Restart activity if theme is changed
        boolean newTheme = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme", false);
        if (currentTheme != newTheme) { //is activity theme changed
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
        super.onResume();
    }
}
