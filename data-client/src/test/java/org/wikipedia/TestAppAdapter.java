package org.wikipedia;

import okhttp3.OkHttpClient;
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

}
