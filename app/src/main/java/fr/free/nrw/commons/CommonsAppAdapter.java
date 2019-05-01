package fr.free.nrw.commons;

import org.wikipedia.AppAdapter;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.login.LoginResult;

import androidx.annotation.NonNull;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import okhttp3.OkHttpClient;

public class CommonsAppAdapter extends AppAdapter {
    private final int DEFAULT_THUMB_SIZE = 640;
    private final String COOKIE_STORE_NAME = "cookie_store";

    private final SessionManager sessionManager;
    private final JsonKvStore preferences;

    CommonsAppAdapter(@NonNull SessionManager sessionManager, @NonNull JsonKvStore preferences) {
        this.sessionManager = sessionManager;
        this.preferences = preferences;
    }

    @Override
    public String getMediaWikiBaseUrl() {
        return BuildConfig.COMMONS_URL;
    }

    @Override
    public String getRestbaseUriFormat() {
        return BuildConfig.COMMONS_URL;
    }

    @Override
    public OkHttpClient getOkHttpClient(@NonNull WikiSite wikiSite) {
        return OkHttpConnectionFactory.getClient();
    }

    @Override
    public int getDesiredLeadImageDp() {
        return DEFAULT_THUMB_SIZE;
    }

    @Override
    public boolean isLoggedIn() {
        return sessionManager.isUserLoggedIn();
    }

    @Override
    public String getUserName() {
        return sessionManager.getUserName();
    }

    @Override
    public String getPassword() {
        return sessionManager.getPassword();
    }

    @Override
    public void updateAccount(@NonNull LoginResult result) {
        // TODO:  sessionManager.updateAccount(result);
    }

    @Override
    public SharedPreferenceCookieManager getCookies() {
        if (!preferences.contains(COOKIE_STORE_NAME)) {
            return null;
        }
        return GsonUnmarshaller.unmarshal(SharedPreferenceCookieManager.class, preferences.getString(COOKIE_STORE_NAME, null));
    }

    @Override
    public void setCookies(@NonNull SharedPreferenceCookieManager cookies) {
        preferences.putString(COOKIE_STORE_NAME, GsonMarshaller.marshal(cookies));
    }

    @Override
    public boolean logErrorsInsteadOfCrashing() {
        return false;
    }
}
