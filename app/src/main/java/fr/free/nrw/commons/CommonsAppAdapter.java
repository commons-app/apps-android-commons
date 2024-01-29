package fr.free.nrw.commons;

import androidx.annotation.NonNull;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import okhttp3.OkHttpClient;
import org.wikipedia.AppAdapter;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;

public class CommonsAppAdapter extends AppAdapter {
    private final String COOKIE_STORE_NAME = "cookie_store";

    private final JsonKvStore preferences;

    CommonsAppAdapter(@NonNull JsonKvStore preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getRestbaseUriFormat() {
        return BuildConfig.COMMONS_URL;
    }

    @Override
    public OkHttpClient getOkHttpClient() {
        return OkHttpConnectionFactory.getClient();
    }

    @Override
    public SharedPreferenceCookieManager getCookies() {
        if (!preferences.contains(COOKIE_STORE_NAME)) {
            return null;
        }
        return GsonUnmarshaller.unmarshal(SharedPreferenceCookieManager.class,
                preferences.getString(COOKIE_STORE_NAME, null));
    }

    @Override
    public void setCookies(@NonNull SharedPreferenceCookieManager cookies) {
        preferences.putString(COOKIE_STORE_NAME, GsonMarshaller.marshal(cookies));
    }

}
