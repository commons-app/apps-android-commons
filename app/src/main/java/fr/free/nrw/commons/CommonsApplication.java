package fr.free.nrw.commons;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.data.Category;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.CommonsApplicationComponent;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.di.DaggerCommonsApplicationComponent;
import fr.free.nrw.commons.modifications.ModifierSequence;
import fr.free.nrw.commons.utils.FileUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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

    @Inject SessionManager sessionManager;
    @Inject DBOpenHelper dbOpenHelper;
    @Inject @Named("default_preferences") SharedPreferences defaultPrefs;
    @Inject @Named("application_preferences") SharedPreferences applicationPrefs;
    @Inject @Named("prefs") SharedPreferences otherPrefs;

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

    public void clearApplicationData(Context context, LogoutListener logoutListener) {
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

        sessionManager.clearAllAccounts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    Timber.d("All accounts have been removed");
                    //TODO: fix preference manager
                    defaultPrefs.edit().clear().commit();
                    applicationPrefs.edit().clear().commit();
                    applicationPrefs.edit().putBoolean("firstrun", false).apply();otherPrefs.edit().clear().commit();
                    updateAllDatabases();

                    logoutListener.onLogoutComplete();
                });
    }

    /**
     * Deletes all tables and re-creates them.
     */
    private void updateAllDatabases() {
        dbOpenHelper.getReadableDatabase().close();
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();

        ModifierSequence.Table.onDelete(db);
        Category.Table.onDelete(db);
        Contribution.Table.onDelete(db);
    }

    public interface LogoutListener {
        void onLogoutComplete();
    }
}
