package fr.free.nrw.commons;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.data.Category;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.CommonsApplicationComponent;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.di.DaggerCommonsApplicationComponent;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
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
public class CommonsApplication extends DaggerApplication {

    @Inject MediaWikiApi mediaWikiApi;
    @Inject AccountUtil accountUtil;
    @Inject DBOpenHelper dbOpenHelper;

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

    private CommonsApplicationComponent component;

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
    }

    @Override
    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return injector();
    }

    public CommonsApplicationComponent injector() {
        if (component == null) {
            component = DaggerCommonsApplicationComponent.builder()
                    .appModule(new CommonsApplicationModule(this))
                    .build();
        }
        return component;
    }

    /**
     * @return Account|null
     */
    public Account getCurrentAccount() {
        if (currentAccount == null) {
            AccountManager accountManager = AccountManager.get(this);
            Account[] allAccounts = accountManager.getAccountsByType(accountUtil.accountType());
            if (allAccounts.length != 0) {
                currentAccount = allAccounts[0];
            }
        }
        return currentAccount;
    }

    public Boolean revalidateAuthToken() {
        AccountManager accountManager = AccountManager.get(this);
        Account curAccount = getCurrentAccount();

        if (curAccount == null) {
            return false; // This should never happen
        }

        accountManager.invalidateAuthToken(accountUtil.accountType(), mediaWikiApi.getAuthCookie());
        try {
            String authCookie = accountManager.blockingGetAuthToken(curAccount, "", false);
            mediaWikiApi.setAuthCookie(authCookie);
            return true;
        } catch (OperationCanceledException | NullPointerException | IOException | AuthenticatorException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void clearApplicationData(Context context, NavigationBaseActivity.LogoutListener logoutListener) {
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
        Account[] allAccounts = accountManager.getAccountsByType(accountUtil.accountType());

        AccountManagerCallback<Boolean> amCallback = new AccountManagerCallback<Boolean>() {

            private int index = 0;

            void setIndex(int index) {
                this.index = index;
            }

            int getIndex() {
                return index;
            }

            @Override
            public void run(AccountManagerFuture<Boolean> accountManagerFuture) {
                setIndex(getIndex() + 1);

                try {
                    if (accountManagerFuture != null) {
                        if (accountManagerFuture.getResult()) {
                            Timber.d("Account removed successfully.");
                        }
                    }
                } catch (OperationCanceledException | IOException | AuthenticatorException e) {
                    e.printStackTrace();
                }

                if (getIndex() == allAccounts.length) {
                    Timber.d("All accounts have been removed");
                    //TODO: fix preference manager
                    PreferenceManager.getDefaultSharedPreferences(CommonsApplication.this).edit().clear().commit();
                    SharedPreferences preferences = context
                            .getSharedPreferences("fr.free.nrw.commons", MODE_PRIVATE);
                    preferences.edit().clear().commit();
                    context.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().clear().commit();
                    preferences.edit().putBoolean("firstrun", false).apply();
                    updateAllDatabases();
                    currentAccount = null;

                    logoutListener.onLogoutComplete();
                }
            }
        };

        for (Account account : allAccounts) {
            accountManager.removeAccount(account, amCallback, null);
        }
    }

    /**
     * Deletes all tables and re-creates them.
     */
    public void updateAllDatabases() {
        dbOpenHelper.getReadableDatabase().close();
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();

        ModifierSequence.Table.onDelete(db);
        Category.Table.onDelete(db);
        Contribution.Table.onDelete(db);
    }
}
