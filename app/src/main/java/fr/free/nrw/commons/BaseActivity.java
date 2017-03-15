package fr.free.nrw.commons;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;


public class BaseActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Check prefs on every activity starts
        if (getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("theme", false)) {
            setTheme(R.style.LightAppTheme);
        }else {
            setTheme(R.style.DarkAppTheme); //default
        }
        super.onCreate(savedInstanceState);
    }
}
