package fr.free.nrw.commons.kvstore;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class JsonKvStore extends BasicKvStore {
    private final Gson gson;

    public JsonKvStore(Context context, String storeName, Gson gson) {
        super(context, storeName);
        this.gson = gson;
    }


    public <T> void putJson(String key, T object) {
        putString(key, gson.toJson(object));
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

}
