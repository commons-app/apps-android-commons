package fr.free.nrw.commons.kvstore;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

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

    /**
     * If you don't want onVersionUpdate to be called on a fresh creation, the first version supplied for the kvstore should be set to 0.
     */
    public BasicKvStore(Context context, String storeName, int version) {
        this(context,storeName,version,false);
    }

    public BasicKvStore(Context context, String storeName, int version, boolean clearAllOnUpgrade) {
        _store = context.getSharedPreferences(storeName, Context.MODE_PRIVATE);
        int oldVersion = getInt(KEY_VERSION);

        if (version > oldVersion) {
            Timber.i("version updated from %s to %s, with clearFlag %b", oldVersion, version, clearAllOnUpgrade);
            onVersionUpdate(oldVersion, version, clearAllOnUpgrade);
        }

        if (version < oldVersion) {
            throw new IllegalArgumentException(
                    "kvstore downgrade not allowed, old version:" + oldVersion + ", new version: " +
                            version);
        }
        //Keep this statement at the end so that clearing of store does not cause version also to get removed.
        putIntInternal(KEY_VERSION, version);
    }

    public void onVersionUpdate(int oldVersion, int version, boolean clearAllFlag) {
        if(clearAllFlag) {
            clearAll();
        }
    }

    public Set<String> getKeySet() {
        Map<String, ?> allContents = new HashMap<>(_store.getAll());
        allContents.remove(KEY_VERSION);
        return allContents.keySet();
    }

    @Nullable
    public Map<String, ?> getAll() {
        Map<String, ?> allContents = _store.getAll();
        if (allContents == null || allContents.size() == 0) {
            return null;
        }
        allContents.remove(KEY_VERSION);
        return new HashMap<>(allContents);
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
    public long getLong(String key) {
        return getLong(key, 0);
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
    public long getLong(String key, long defaultValue) {
        return _store.getLong(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return _store.getInt(key, defaultValue);
    }

    public void putAllStrings(Map<String, String> keyValuePairs) {
        SharedPreferences.Editor editor = _store.edit();
        for (Map.Entry<String, String> keyValuePair : keyValuePairs.entrySet()) {
            putString(editor, keyValuePair.getKey(), keyValuePair.getValue(), false);
        }
        editor.apply();
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
    public void putLong(String key, long value) {
        assertKeyNotReserved(key);
        SharedPreferences.Editor editor = _store.edit();
        editor.putLong(key, value);
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

    @Override
    public void clearAllWithVersion() {
        SharedPreferences.Editor editor = _store.edit();
        editor.clear();
        editor.apply();
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

    public void registerChangeListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        _store.registerOnSharedPreferenceChangeListener(l);
    }

    public void unregisterChangeListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        _store.unregisterOnSharedPreferenceChangeListener(l);
    }

}