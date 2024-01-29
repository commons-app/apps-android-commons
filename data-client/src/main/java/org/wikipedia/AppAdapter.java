package org.wikipedia;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.SharedPreferenceCookieManager;

import okhttp3.OkHttpClient;

public abstract class AppAdapter {
    private final int DEFAULT_THUMB_SIZE = 640;

    public abstract OkHttpClient getOkHttpClient();

    // Unused from commons app, implement here as a temporary step during refactoring
    public String getRestbaseUriFormat() {
        return "";
    }

    // Unused from commons app, implement here as a temporary step during refactoring
    public int getDesiredLeadImageDp() {
        return DEFAULT_THUMB_SIZE;
    }

    public abstract SharedPreferenceCookieManager getCookies();
    public abstract void setCookies(@NonNull SharedPreferenceCookieManager cookies);

    private static AppAdapter INSTANCE;
    public static void set(AppAdapter instance) {
        INSTANCE = instance;
    }
    public static AppAdapter get() {
        if (INSTANCE == null) {
            throw new RuntimeException("Please provide an instance of AppAdapter when using this library.");
        }
        return INSTANCE;
    }
}
