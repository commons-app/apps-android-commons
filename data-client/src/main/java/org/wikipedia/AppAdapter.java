package org.wikipedia;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.SharedPreferenceCookieManager;

import okhttp3.OkHttpClient;

public abstract class AppAdapter {

    public abstract String getMediaWikiBaseUrl();
    public abstract String getRestbaseUriFormat();
    public abstract OkHttpClient getOkHttpClient();
    public abstract int getDesiredLeadImageDp();

    public abstract boolean isLoggedIn();
    public abstract String getUserName();
    public abstract String getPassword();

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
