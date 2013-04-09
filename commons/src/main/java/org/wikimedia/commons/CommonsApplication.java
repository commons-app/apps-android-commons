package org.wikimedia.commons;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;

import android.accounts.*;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.cache.memory.impl.LimitedAgeMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.client.HttpClient;
import org.mediawiki.api.*;
import org.wikimedia.commons.auth.WikiAccountAuthenticator;
import org.apache.http.impl.client.*;
import org.apache.http.params.CoreProtocolPNames;
import org.wikimedia.commons.data.*;

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
    public static final String HOME_URL = "https://test.wikipedia.org/wiki/";
    public static final String EVENTLOG_URL = "https://bits.wikimedia.org/event.gif";
    public static final String EVENTLOG_WIKI = "testwiki";

    public static final Object[] EVENT_UPLOAD_ATTEMPT = {"MobileAppUploadAttempts", 5334329L};
    public static final Object[] EVENT_LOGIN_ATTEMPT = {"MobileAppLoginAttempts", 5257721L};
    public static final Object[] EVENT_SHARE_ATTEMPT = {"MobileAppShareAttempts", 5346170L};
    public static final Object[] EVENT_CATEGORIZATION_ATTEMPT = {"MobileAppCategorizationAttempts", 5359208L};
    public static final Object[] EVENT_EVENTLOGGING_CHANGE = {"MobileAppTrackingChange", 5369400L};

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using Android Commons app";

    public static final String FEEDBACK_EMAIL = "mobile-feedback-l@lists.wikimedia.org";
    public static final String FEEDBACK_EMAIL_SUBJECT = "Commons Android App (%s) Feedback";


    public static AbstractHttpClient createHttpClient() {
        DefaultHttpClient client = new DefaultHttpClient();
        client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, "Commons/" + APPLICATION_VERSION + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE);
        return client;
    }

    public static MWApi createMWApi() {
        return new MWApi(API_URL, createHttpClient());
    }


    public DBOpenHelper getDbOpenHelper() {
        if(dbOpenHelper == null) {
            dbOpenHelper = new DBOpenHelper(this);
        }
        return dbOpenHelper;
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
                .build();
        ImageLoader.getInstance().init(imageLoaderConfiguration);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            APPLICATION_VERSION = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // LET US WIN THE AWARD FOR DUMBEST CHECKED EXCEPTION EVER!
            throw new RuntimeException(e);
        }

        // Initialize EventLogging
        EventLog.setApp(this);
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
