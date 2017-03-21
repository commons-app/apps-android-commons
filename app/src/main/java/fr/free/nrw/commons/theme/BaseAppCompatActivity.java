package fr.free.nrw.commons.theme;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import fr.free.nrw.commons.R;

public class BaseAppCompatActivity extends AppCompatActivity {
    boolean currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme",true)) {
            currentTheme = true;
            setTheme(R.style.DarkAppTheme);
        }else {
            currentTheme = false;
            setTheme(R.style.LightAppTheme); // default
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        // Restart activity if theme is changed
        boolean newTheme = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("theme",true);
        if(currentTheme!=newTheme){ //is activity theme changed
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        }
        super.onResume();
    }
}
