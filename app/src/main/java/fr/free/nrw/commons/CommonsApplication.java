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
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import java.io.IOException;

import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.caching.CacheController;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.data.Category;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.mwapi.ApacheHttpClientMediaWikiApi;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
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

    public static final Object[] EVENT_UPLOAD_ATTEMPT = {"MobileAppUploadAttempts", 5334329L};
    public static final Object[] EVENT_LOGIN_ATTEMPT = {"MobileAppLoginAttempts", 5257721L};
    public static final Object[] EVENT_SHARE_ATTEMPT = {"MobileAppShareAttempts", 5346170L};
    public static final Object[] EVENT_CATEGORIZATION_ATTEMPT = {"MobileAppCategorizationAttempts", 5359208L};

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using Android Commons app";

    public static final String FEEDBACK_EMAIL = "commons-app-android@googlegroups.com";
    public static final String FEEDBACK_EMAIL_SUBJECT = "Commons Android App (%s) Feedback";

    private static CommonsApplication instance = null;
    private MediaWikiApi api = null;
    private LruCache<String, String> thumbnailUrlCache = new LruCache<>(1024);
    private CacheController cacheData = null;
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

    public MediaWikiApi getMWApi() {
        if (api == null) {
            api = new ApacheHttpClientMediaWikiApi(BuildConfig.WIKIMEDIA_API_HOST);
        }
        return api;
    }

    public CacheController getCacheData() {
        if (cacheData == null) {
            cacheData = new CacheController();
        }
        return cacheData;
    }

    public LruCache<String, String> getThumbnailUrlCache() {
        return thumbnailUrlCache;
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
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        Timber.plant(new Timber.DebugTree());



        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        } else {
            Stetho.initializeWithDefaults(this);
        }

        // Fire progress callbacks for every 3% of uploaded content
        System.setProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "3.0");

        Fresco.initialize(this);

        //For caching area -> categories
        cacheData  = new CacheController();
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
        for (Account allAccount : allAccounts) {
            accountManager.removeAccount(allAccount, null, null);
        }

        //TODO: fix preference manager 
        PreferenceManager.getDefaultSharedPreferences(getInstance()).edit().clear().commit();
        SharedPreferences preferences = context
                .getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);
        preferences.edit().clear().commit();
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().clear().commit();
        preferences.edit().putBoolean("firstrun", false).apply();
        updateAllDatabases();
        currentAccount = null;
    }

    /**
     * Deletes all tables and re-creates them.
     */
    public void updateAllDatabases() {
        DBOpenHelper dbOpenHelper = CommonsApplication.getInstance().getDBOpenHelper();
        dbOpenHelper.getReadableDatabase().close();
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();

        ModifierSequence.Table.onDelete(db);
        Category.Table.onDelete(db);
        Contribution.Table.onDelete(db);
    }
}
