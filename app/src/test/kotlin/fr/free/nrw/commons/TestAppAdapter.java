package fr.free.nrw.commons;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import org.wikipedia.AppAdapter;
import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.okhttp.TestStubInterceptor;
import org.wikipedia.dataclient.okhttp.UnsuccessfulResponseInterceptor;

public class TestAppAdapter extends AppAdapter {

    @Override
    public OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
            .addInterceptor(new UnsuccessfulResponseInterceptor())
            .addInterceptor(new TestStubInterceptor())
            .build();
    }

    @Override
    public SharedPreferenceCookieManager getCookies() {
        return null;
    }

    @Override
    public void setCookies(@NonNull SharedPreferenceCookieManager cookies) {
    }

}

