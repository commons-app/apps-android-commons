package fr.free.nrw.commons.feedback;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.wikipedia.dataclient.Service;
import retrofit2.Retrofit;

public class FeedbackClient {
    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if(retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor).build();

            retrofit = new Retrofit.Builder()
                .client(client)
                .baseUrl(Service.COMMONS_URL).build();
        }
        return retrofit;
    }
}
