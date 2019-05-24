package fr.free.nrw.commons.kvstore;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JsonKvStore extends BasicKvStore {
    private final Gson gson;

    public JsonKvStore(Context context, String storeName, Gson gson) {
        super(context, storeName);
        this.gson = gson;
    }

    public JsonKvStore(Context context, String storeName, int version, Gson gson) {
        super(context, storeName, version);
        this.gson = gson;
    }

    public JsonKvStore(Context context, String storeName, int version, boolean clearAllOnUpgrade, Gson gson) {
        super(context, storeName, version, clearAllOnUpgrade);
        this.gson = gson;
    }

    public <T> void putAllJsons(Map<String, T> jsonMap) {
        Map<String, String> stringsMap = new HashMap<>(jsonMap.size());
        for (Map.Entry<String, T> keyValuePair : jsonMap.entrySet()) {
            String jsonString = gson.toJson(keyValuePair.getValue());
            stringsMap.put(keyValuePair.getKey(), jsonString);
        }
        putAllStrings(stringsMap);
    }

    public <T> void putJson(String key, T object) {
        putString(key, gson.toJson(object));
    }

    public <T> void putJsonWithTypeInfo(String key, T object, Type type) {
        putString(key, gson.toJson(object, type));
    }

    @Nullable
    public <T> T getJson(String key, Class<T> clazz) {
        String jsonString = getString(key);
        try {
            return gson.fromJson(jsonString, clazz);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    @Nullable
    public <T> T getJson(String key, Type type) {
        String jsonString = getString(key);
        try {
            return gson.fromJson(jsonString, type);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }
}