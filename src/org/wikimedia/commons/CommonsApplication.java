package org.wikimedia.commons;

import android.app.Application;
import org.mediawiki.api.*;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

public class CommonsApplication extends Application {

    private MWApi api;
    public static final String API_URL = "http://test.wikipedia.org/w/api.php";
   
    public static MWApi createMWApi() {
        DefaultHttpClient client = new DefaultHttpClient();
        // Because WMF servers support only HTTP/1.0. Biggest difference that
        // this makes is support for Chunked Transfer Encoding. 
        // I have this here so if any 1.1 features start being used, it 
        // throws up. 
        client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, 
                HttpVersion.HTTP_1_0);
        return new MWApi(API_URL, client);
    }
    
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        api = createMWApi();
    }
    
    public MWApi getApi() {
        return api;
    }

}
