package fr.free.nrw.commons.kvstore;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class BasicKvStore implements KeyValueStore {
    private static final String KEY_VERSION = "__version__";
    /*
    This class only performs puts, sets and clears.
    A commit returns a boolean indicating whether it has succeeded, we are not throwing an exception as it will
    require the dev to handle it in every usage - instead we will pass on this boolean so it can be evaluated if needed.
    */
    private final SharedPreferences _store;

    public BasicKvStore(Context context, String storeName) {
        _store = context.getSharedPreferences(storeName, Context.MODE_PRIVATE);
    }

    @Override
    public String getString(String key) {
        return getString(key, null);
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public int getInt(String key) {
        return getInt(key, 0);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return _store.getString(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return _store.getBoolean(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return _store.getInt(key, defaultValue);
    }

    @Override
    public void putString(String key, String value) {
        SharedPreferences.Editor editor = _store.edit();
        putString(editor, key, value, true);
    }

    private void putString(SharedPreferences.Editor editor, String key, String value,
                           boolean commit) {
        assertKeyNotReserved(key);
        editor.putString(key, value);
        if(commit) {
            editor.apply();
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        assertKeyNotReserved(key);
        SharedPreferences.Editor editor = _store.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    @Override
    public void putInt(String key, int value) {
        assertKeyNotReserved(key);
        putIntInternal(key, value);
    }

    @Override
    public boolean contains(String key) {
        return _store.contains(key);
    }

    @Override
    public void remove(String key) {
        SharedPreferences.Editor editor = _store.edit();
        editor.remove(key);
        editor.apply();
    }

    @Override
    public void clearAll() {
        int version = getInt(KEY_VERSION);
        SharedPreferences.Editor editor = _store.edit();
        editor.clear();
        editor.apply();
        putIntInternal(KEY_VERSION, version);
    }

    private void putIntInternal(String key, int value) {
        SharedPreferences.Editor editor = _store.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void assertKeyNotReserved(String key) {
        if (key.equals(KEY_VERSION)) {
            throw new IllegalArgumentException(key + "is a reserved key");
        }
    }

    public Set<String> getStringSet(String key){
        return _store.getStringSet(key, new HashSet<>());
    }

    public void putStringSet(String key,Set<String> value){
        _store.edit().putStringSet(key,value).apply();
    }
}
