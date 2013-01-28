package org.wikimedia.commons;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.*;

import android.accounts.*;
import android.app.Application;
import android.os.AsyncTask;
import android.os.Build;

import org.mediawiki.api.*;
import org.w3c.dom.Node;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

public class CommonsApplication extends Application {

    private MWApi api;
    private Account currentAccount = null; // Unlike a savings account...
    public static final String API_URL = "http://test.wikipedia.org/w/api.php";
   
    public static MWApi createMWApi() {
        DefaultHttpClient client = new DefaultHttpClient();
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
    
    public Account getCurrentAccount() {
        if(currentAccount == null) {
            AccountManager accountManager = AccountManager.get(this);
            Account[] allAccounts = accountManager.getAccountsByType(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE);
            if(allAccounts.length != 0) {
                currentAccount = allAccounts[0];
            }
        }
        return currentAccount;
    }
    
    public Boolean revalidateAuthToken() {
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = getCurrentAccount();
       
        if(curAccount == null) {
            return false; // This should never happen
        }
        
        accountManager.invalidateAuthToken(WikiAccountAuthenticator.COMMONS_ACCOUNT_TYPE, api.getAuthCookie());
        try {
            String authCookie = accountManager.blockingGetAuthToken(curAccount, "", false);
            api.setAuthCookie(authCookie);
            return true;
        } catch (OperationCanceledException e) {
            e.printStackTrace();
            return false;
        } catch (AuthenticatorException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getStringFromDOM(Node dom) {
       javax.xml.transform.Transformer transformer = null;
       try {
           transformer = TransformerFactory.newInstance().newTransformer();
       } catch (TransformerConfigurationException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       } catch (TransformerFactoryConfigurationError e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }

       StringWriter  outputStream = new StringWriter();
       javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(dom);
       javax.xml.transform.stream.StreamResult strResult = new javax.xml.transform.stream.StreamResult(outputStream);

       try {
        transformer.transform(domSource, strResult);
       } catch (TransformerException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       } 
       return outputStream.toString();
    }
    
    static public <T> void executeAsyncTask(AsyncTask<T, ?, ?> task,
            T... params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }
        else {
            task.execute(params);
        }
    } 
}
