package fr.free.nrw.commons.network;

import android.support.annotation.NonNull;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitFactory {

    public static Retrofit newInstance(@NonNull String endpoint) {
        return new Retrofit.Builder()
                .client(new OkHttpClient())
                .baseUrl(endpoint)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.getDefaultGson()))
                .build();
    }

    private RetrofitFactory() {
    }
}

