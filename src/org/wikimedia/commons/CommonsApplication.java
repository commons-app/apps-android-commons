package org.wikimedia.commons;

import android.app.Application;
import org.mediawiki.api.*;
import org.apache.http.impl.client.DefaultHttpClient;

public class CommonsApplication extends Application {

    private MWApi api;
    
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        DefaultHttpClient client = new DefaultHttpClient();
        api = new MWApi("http://192.168.1.34/w/api.php", client);
    }
    
    public MWApi getApi() {
        return api;
    }

}
