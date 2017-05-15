package fr.free.nrw.commons;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Application;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import fr.free.nrw.commons.auth.AccountUtil;
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;

import java.io.IOException;

import fr.free.nrw.commons.caching.CacheController;
import timber.log.Timber;

// TODO: Use ProGuard to rip out reporting when publishing
@ReportsCrashes(
        mailTo = "commons-app-android-private@googlegroups.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)
public class CommonsApplication extends Application {

    private MWApi api;
    private Account currentAccount = null; // Unlike a savings account...

    public static String ROOT_URL = "https://commons.wikimedia.org";
    public static String ROOT_MOBILE_URL = "https://commons.m.wikimedia.org";
    public static String IMAGE_URL_BASE = "https://upload.wikimedia.org/wikipedia/commons";
    // Set dynamically from ROOT_URL and ROOT_MOBILE_URL
    public static String API_URL;
    public static String SCRIPT_URL;
    public static String HOME_URL;
    public static String MOBILE_HOME_URL;
    public static String EVENTLOG_URL;

    public static final String EVENTLOG_WIKI = "commonswiki";

    public static final Object[] EVENT_UPLOAD_ATTEMPT = {"MobileAppUploadAttempts", 5334329L};
    public static final Object[] EVENT_LOGIN_ATTEMPT = {"MobileAppLoginAttempts", 5257721L};
    public static final Object[] EVENT_SHARE_ATTEMPT = {"MobileAppShareAttempts", 5346170L};
    public static final Object[] EVENT_CATEGORIZATION_ATTEMPT = {"MobileAppCategorizationAttempts", 5359208L};

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using Android Commons app";

    public static final String FEEDBACK_EMAIL = "commons-app-android@googlegroups.com";
    public static final String FEEDBACK_EMAIL_SUBJECT = "Commons Android App (%s) Feedback";

    public RequestQueue volleyQueue;

    public CacheController cacheData;

    public static CommonsApplication app;

    public static AbstractHttpClient createHttpClient() {
        BasicHttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        params.setParameter(CoreProtocolPNames.USER_AGENT, "Commons/" + BuildConfig.VERSION_NAME + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE);
        return new DefaultHttpClient(cm, params);
    }

    public static MWApi createMWApi() {
        return new MWApi(API_URL, createHttpClient());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        Timber.plant(new Timber.DebugTree());

        if (BuildConfig.DEBUG) {
            ROOT_URL = "https://commons.wikimedia.beta.wmflabs.org";
            ROOT_MOBILE_URL = "https://commons.m.wikimedia.beta.wmflabs.org";
            IMAGE_URL_BASE = "https://upload.beta.wmflabs.org/wikipedia/commons";
        } else {
            // Not debugging
            ACRA.init(this);
        }

        HOME_URL = ROOT_URL + "/wiki/";
        MOBILE_HOME_URL = ROOT_MOBILE_URL + "/wiki/";
        SCRIPT_URL = ROOT_URL + "/w/";
        API_URL = SCRIPT_URL + "api.php";
        // Defined in https://noc.wikimedia.org/conf/CommonSettings.php.txt
        EVENTLOG_URL = ROOT_URL + "/beacon/event";

        // Fire progress callbacks for every 3% of uploaded content
        System.setProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "3.0");
        api = createMWApi();

        ImageLoaderConfiguration imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .discCache(new TotalSizeLimitedDiscCache(StorageUtils.getCacheDirectory(this), 128 * 1024 * 1024))
                .build();
        ImageLoader.getInstance().init(imageLoaderConfiguration);

        // Initialize EventLogging
        EventLog.setApp(this);

        // based off https://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
        // Cache for 1/8th of available VM memory
        long maxMem = Runtime.getRuntime().maxMemory();
        if (maxMem < 48L * 1024L * 1024L) {
            // Cache only one bitmap if VM memory is too small (such as Nexus One);
            Timber.d("Skipping bitmap cache; max mem is: %d", maxMem);
            imageCache = new LruCache<>(1);
        } else {
            int cacheSize = (int) (maxMem / (1024 * 8));
            Timber.d("Bitmap cache size %d from max mem %d", cacheSize, maxMem);
            imageCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    int bitmapSize;
                    bitmapSize = bitmap.getByteCount();

                    // The cache size will be measured in kilobytes rather than number of items.
                    return bitmapSize / 1024;
                }
            };
        }

        //For caching area -> categories
        cacheData  = new CacheController();

        DiskBasedCache cache = new DiskBasedCache(getCacheDir(), 16 * 1024 * 1024);
        volleyQueue = new RequestQueue(cache, new BasicNetwork(new HurlStack()));
        volleyQueue.start();
    }

    private com.android.volley.toolbox.ImageLoader imageLoader;
    private LruCache<String, Bitmap> imageCache;

    public com.android.volley.toolbox.ImageLoader getImageLoader() {
        if(imageLoader == null) {
            imageLoader = new com.android.volley.toolbox.ImageLoader(volleyQueue, new com.android.volley.toolbox.ImageLoader.ImageCache() {
                @Override
                public Bitmap getBitmap(String key) {
                    return imageCache.get(key);
                }

                @Override
                public void putBitmap(String key, Bitmap bitmap) {
                    imageCache.put(key, bitmap);
                }
            });
            imageLoader.setBatchedResponseDelay(0);
        }
        return imageLoader;
    }
    
    public MWApi getApi() {
        return api;
    }

    /**
     * @return Accout|null
     */
    public Account getCurrentAccount() {
        if(currentAccount == null) {
            AccountManager accountManager = AccountManager.get(this);
            Account[] allAccounts = accountManager.getAccountsByType(AccountUtil.accountType());
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
        
        accountManager.invalidateAuthToken(AccountUtil.accountType(), api.getAuthCookie());
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
        } catch (NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deviceHasCamera() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }
}
