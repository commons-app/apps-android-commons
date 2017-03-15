package fr.free.nrw.commons;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class BaseAppCompatActivity extends AppCompatActivity {
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
