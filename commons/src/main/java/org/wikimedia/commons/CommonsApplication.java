package org.wikimedia.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.transform.*;

import android.accounts.*;
import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.download.HttpClientImageDownloader;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.nostra13.universalimageloader.core.download.URLConnectionImageDownloader;
import com.nostra13.universalimageloader.utils.StorageUtils;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.client.HttpClient;
import org.mediawiki.api.*;
import org.w3c.dom.Node;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.wikimedia.commons.data.DBOpenHelper;

// TODO: Use ProGuard to rip out reporting when publishing
@ReportsCrashes(formKey = "",
        mailTo = "yuvipanda@wikimedia.org",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)
public class CommonsApplication extends Application {

    private DBOpenHelper dbOpenHelper;
    public static String APPLICATION_VERSION; // Populated in onCreate. Race conditions theoretically possible, but practically not?

    private MWApi api;
    private Account currentAccount = null; // Unlike a savings account...
    public static final String API_URL = "https://test.wikipedia.org/w/api.php";
    public static final String IMAGE_URL_BASE = "https://upload.wikimedia.org/wikipedia/test";
    public static final String EVENTLOG_URL = "https://bits.wikimedia.org/event.gif";

    public static final Object[] EVENT_UPLOAD_ATTEMPT = {"MobileAppUploadAttempts", 5241449L};
    public static final Object[] EVENT_LOGIN_ATTEMPT = {"MobileAppLoginAttempts", 5240393L};

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using Android Commons app";


    public static MWApi createMWApi() {
        DefaultHttpClient client = new DefaultHttpClient();
        return new MWApi(API_URL, client);
    }


    public DBOpenHelper getDbOpenHelper() {
        if(dbOpenHelper == null) {
            dbOpenHelper = new DBOpenHelper(this);
        }
        return dbOpenHelper;
    }

    public class ContentUriImageDownloader extends URLConnectionImageDownloader {
        @Override
        protected InputStream getStreamFromOtherSource(URI imageUri) throws IOException {
            if(imageUri.getScheme().equals("content")) {
                return getContentResolver().openInputStream(Uri.parse(imageUri.toString()));
            }
            throw new RuntimeException("Not a content URI: " + imageUri);
        }
    }

    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
        // Fire progress callbacks for every 3% of uploaded content
        System.setProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "3.0");
        api = createMWApi();


        ImageLoaderConfiguration imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .discCache(new TotalSizeLimitedDiscCache(StorageUtils.getCacheDirectory(this), 128 * 1024 * 1024))
                .imageDownloader(new ContentUriImageDownloader()).build();
        ImageLoader.getInstance().init(imageLoaderConfiguration);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            APPLICATION_VERSION = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // LET US WIN THE AWARD FOR DUMBEST CHECKED EXCEPTION EVER!
            throw new RuntimeException(e);
        }
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


}
