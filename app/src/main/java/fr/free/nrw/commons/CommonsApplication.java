package fr.free.nrw.commons;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.facebook.stetho.Stetho;
import com.nostra13.universalimageloader.cache.disc.impl.TotalSizeLimitedDiscCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.category.Category;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.nearby.NearbyPlaces;

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

import java.io.File;
import java.io.IOException;

import fr.free.nrw.commons.utils.FileUtils;
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

    private Account currentAccount = null; // Unlike a savings account...
    public static final String API_URL = "https://commons.wikimedia.org/w/api.php";
    public static final String IMAGE_URL_BASE = "https://upload.wikimedia.org/wikipedia/commons";
    public static final String HOME_URL = "https://commons.wikimedia.org/wiki/";
    public static final String MOBILE_HOME_URL = "https://commons.m.wikimedia.org/wiki/";
    public static final String EVENTLOG_URL = "https://www.wikimedia.org/beacon/event";
    public static final String EVENTLOG_WIKI = "commonswiki";

    public static final Object[] EVENT_UPLOAD_ATTEMPT = {"MobileAppUploadAttempts", 5334329L};
    public static final Object[] EVENT_LOGIN_ATTEMPT = {"MobileAppLoginAttempts", 5257721L};
    public static final Object[] EVENT_SHARE_ATTEMPT = {"MobileAppShareAttempts", 5346170L};
    public static final Object[] EVENT_CATEGORIZATION_ATTEMPT = {"MobileAppCategorizationAttempts", 5359208L};

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using Android Commons app";

    public static final String FEEDBACK_EMAIL = "commons-app-android@googlegroups.com";
    public static final String FEEDBACK_EMAIL_SUBJECT = "Commons Android App (%s) Feedback";

    private static CommonsApplication instance = null;
    private AbstractHttpClient httpClient = null;
    private MWApi api = null;
    private CacheController cacheData = null;
    private RequestQueue volleyQueue = null;
    private DBOpenHelper dbOpenHelper = null;
    private NearbyPlaces nearbyPlaces = null;

    /**
     * This should not be called by ANY application code (other than the magic Android glue)
     * Use CommonsApplication.getInstance() instead to get the singleton.
     */
    public CommonsApplication() {
        CommonsApplication.instance = this;
    }

    public static CommonsApplication getInstance() {
        if (instance == null) {
            instance = new CommonsApplication();
        }
        return instance;
    }

    public AbstractHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = newHttpClient();
        }
        return httpClient;
    }

    private AbstractHttpClient newHttpClient() {
        BasicHttpParams params = new BasicHttpParams();
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
        schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        params.setParameter(CoreProtocolPNames.USER_AGENT, "Commons/" + BuildConfig.VERSION_NAME + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE);
        return new DefaultHttpClient(cm, params);
    }

    public MWApi getMWApi() {
        if (api == null) {
            api = newMWApi();
        }
        return api;
    }

    private MWApi newMWApi() {
        return new MWApi(API_URL, getHttpClient());
    }

    public CacheController getCacheData() {
        if (cacheData == null) {
            cacheData = new CacheController();
        }
        return cacheData;
    }

    public RequestQueue getVolleyQueue() {
        if (volleyQueue == null) {
            DiskBasedCache cache = new DiskBasedCache(getCacheDir(), 16 * 1024 * 1024);
            volleyQueue = new RequestQueue(cache, new BasicNetwork(new HurlStack()));
            volleyQueue.start();
        }
        return volleyQueue;
    }

    public synchronized DBOpenHelper getDBOpenHelper() {
        if (dbOpenHelper == null) {
            dbOpenHelper = new DBOpenHelper(this);
        }
        return dbOpenHelper;
    }

    public synchronized NearbyPlaces getNearbyPlaces() {
        if (nearbyPlaces == null) {
            nearbyPlaces = new NearbyPlaces();
        }
        return nearbyPlaces;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        Stetho.initializeWithDefaults(this);

        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        }
        // Fire progress callbacks for every 3% of uploaded content
        System.setProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "3.0");

        ImageLoaderConfiguration imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(this)
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
    }

    private com.android.volley.toolbox.ImageLoader imageLoader;
    private LruCache<String, Bitmap> imageCache;

    public com.android.volley.toolbox.ImageLoader getImageLoader() {
        if(imageLoader == null) {
            imageLoader = new com.android.volley.toolbox.ImageLoader(getVolleyQueue(), new com.android.volley.toolbox.ImageLoader.ImageCache() {
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

    /**
     * @return Account|null
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
        
        accountManager.invalidateAuthToken(AccountUtil.accountType(), getMWApi().getAuthCookie());
        try {
            String authCookie = accountManager.blockingGetAuthToken(curAccount, "", false);
            getMWApi().setAuthCookie(authCookie);
            return true;
        } catch (OperationCanceledException | NullPointerException | IOException | AuthenticatorException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deviceHasCamera() {
        PackageManager pm = getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    public void clearApplicationData(Context context) {
        File cacheDirectory = context.getCacheDir();
        File applicationDirectory = new File(cacheDirectory.getParent());
        if (applicationDirectory.exists()) {
            String[] fileNames = applicationDirectory.list();
            for (String fileName : fileNames) {
                if (!fileName.equals("lib")) {
                    FileUtils.deleteFile(new File(applicationDirectory, fileName));
                }
            }
        }

        AccountManager accountManager = AccountManager.get(this);
        Account[] allAccounts = accountManager.getAccountsByType(AccountUtil.accountType());
        for (int index = 0; index < allAccounts.length; index++) {
            accountManager.removeAccount(allAccounts[index], null, null);
        }

        //TODO: fix preference manager 
        PreferenceManager.getDefaultSharedPreferences(getInstance()).edit().clear().commit();
        SharedPreferences preferences = context
                .getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);
        preferences.edit().clear().commit();
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().clear().commit();
        preferences.edit().putBoolean("firstrun", false).apply();
        updateAllDatabases(context);
        currentAccount = null;
    }

    /**
     * Deletes all tables and re-creates them.
     * @param context context
     */
    public void updateAllDatabases(Context context) {
        DBOpenHelper dbOpenHelper = CommonsApplication.getInstance().getDBOpenHelper();
        dbOpenHelper.getReadableDatabase().close();
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();

        ModifierSequence.Table.onDelete(db);
        Category.Table.onDelete(db);
        Contribution.Table.onDelete(db);
    }
}
