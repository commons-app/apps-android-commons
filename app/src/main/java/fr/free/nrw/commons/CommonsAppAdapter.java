package fr.free.nrw.commons;

import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar;
import okhttp3.OkHttpClient;
import org.wikipedia.AppAdapter;

public class CommonsAppAdapter extends AppAdapter {
    private final CommonsCookieJar cookieJar;

    CommonsAppAdapter(final CommonsCookieJar cookieJar) {
        this.cookieJar = cookieJar;
    }

    @Override
    public OkHttpClient getOkHttpClient() {
        return OkHttpConnectionFactory.getClient(cookieJar);
    }

}
