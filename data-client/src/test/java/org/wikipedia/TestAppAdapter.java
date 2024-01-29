package org.wikipedia;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.SharedPreferenceCookieManager;
import org.wikipedia.dataclient.okhttp.TestStubInterceptor;
import org.wikipedia.dataclient.okhttp.UnsuccessfulResponseInterceptor;
import org.wikipedia.login.LoginResult;

import okhttp3.OkHttpClient;

public class TestAppAdapter extends AppAdapter {

    @Override
    public String getRestbaseUriFormat() {
        return "%1$s://%2$s/api/rest_v1/";
    }

    @Override
    public OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new UnsuccessfulResponseInterceptor())
                .addInterceptor(new TestStubInterceptor())
                .build();
    }

    @Override
    public String getUserName() {
        return null;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public void updateAccount(@NonNull LoginResult result) {
    }

    @Override
    public SharedPreferenceCookieManager getCookies() {
        return null;
    }

    @Override
    public void setCookies(@NonNull SharedPreferenceCookieManager cookies) {
    }

}
