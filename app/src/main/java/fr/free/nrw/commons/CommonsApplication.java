package fr.free.nrw.commons;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.multidex.MultiDexApplication;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.category.CategoryDao;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;
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
public class CommonsApplication extends MultiDexApplication {

    @Inject SessionManager sessionManager;
    @Inject DBOpenHelper dbOpenHelper;

    @Inject @Named("default_preferences") SharedPreferences defaultPrefs;
    @Inject @Named("application_preferences") SharedPreferences applicationPrefs;
    @Inject @Named("prefs") SharedPreferences otherPrefs;

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using [[COM:MOA|Commons Mobile App]]";

    public static final String FEEDBACK_EMAIL = "commons-app-android@googlegroups.com";

    public static final String FEEDBACK_EMAIL_SUBJECT = "Commons Android App (%s) Feedback";

    public static final String LOGS_PRIVATE_EMAIL = "commons-app-android-private@googlegroups.com";

    public static final String LOGS_PRIVATE_EMAIL_SUBJECT = "Commons Android App (%s) Logs";

    private RefWatcher refWatcher;


    /**
     * Used to declare and initialize various components and dependencies
     */
    @Override
    public void onCreate() {
        super.onCreate();

        ApplicationlessInjection
                .getInstance(this)
                .getCommonsApplicationComponent()
                .inject(this);
//        Set DownsampleEnabled to True to downsample the image in case it's heavy
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setDownsampleEnabled(true)
                .build();
        Fresco.initialize(this,config);
        if (setupLeakCanary() == RefWatcher.DISABLED) {
            return;
        }

        Timber.plant(new Timber.DebugTree());

        if (!BuildConfig.DEBUG) {
            ACRA.init(this);
        } else {
            Stetho.initializeWithDefaults(this);
        }

        // Fire progress callbacks for every 3% of uploaded content
        System.setProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "3.0");
    }


    /**
     * Helps in setting up LeakCanary library
     * @return instance of LeakCanary
     */
    protected RefWatcher setupLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return RefWatcher.DISABLED;
        }
        return LeakCanary.install(this);
    }

  /**
     * Provides a way to get member refWatcher
     *
     * @param context Application context
     * @return application member refWatcher
     */
    public static RefWatcher getRefWatcher(Context context) {
        CommonsApplication application = (CommonsApplication) context.getApplicationContext();
        return application.refWatcher;
    }

    /**
     * clears data of current application
     * @param context Application context
     * @param logoutListener Implementation of interface LogoutListener
     */
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
                    defaultPrefs.edit().clear().apply();
                    applicationPrefs.edit().clear().apply();
                    applicationPrefs.edit().putBoolean("firstrun", false).apply();
                    otherPrefs.edit().clear().apply();
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

        ModifierSequenceDao.Table.onDelete(db);
        CategoryDao.Table.onDelete(db);
        ContributionDao.Table.onDelete(db);
    }

    /**
     * Interface used to get log-out events
     */
    public interface LogoutListener {
        void onLogoutComplete();
    }
}
