package fr.free.nrw.commons.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;

public class CommonsAppSharedPref {
    private SharedPreferences sharedPreferences;
    private static CommonsAppSharedPref instance;

    private CommonsAppSharedPref(Context context){
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static CommonsAppSharedPref getInstance(Context context){
        if (instance == null) {
            instance =  new CommonsAppSharedPref(context);
        }
        return instance;
    }

    public String getPreferenceString(String key, String defaultValue){
        return sharedPreferences.getString(key, defaultValue);
    }

    public void putPreferenceString(String key, String value){
        sharedPreferences.edit().putString(key, value).commit();
    }

    public boolean getPreferenceBoolean(String key, boolean defaultValue){
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void putPreferenceBoolean(String key, boolean value){
        sharedPreferences.edit().putBoolean(key, value).commit();
    }

    public int getPreferenceInt(String key, int defaultValue){
        return sharedPreferences.getInt(key, defaultValue);
    }

    public void putPreferenceInt(String key, int value){
        sharedPreferences.edit().putInt(key, value).commit();
    }

    public Map<String, ?> getAllPreferences(){
        return sharedPreferences.getAll();
    }
}
