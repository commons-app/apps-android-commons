package fr.free.nrw.commons;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.tspoon.traceur.Traceur;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao;
import fr.free.nrw.commons.category.CategoryDao;
import fr.free.nrw.commons.concurrency.BackgroundPoolExceptionHandler;
import fr.free.nrw.commons.concurrency.ThreadPoolService;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.logging.FileLoggingTree;
import fr.free.nrw.commons.logging.LogUtils;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.ContributionUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@ReportsCrashes(
        mailTo = "commons-app-android-private@googlegroups.com",
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_dialog_text,
        resDialogTitle = R.string.crash_dialog_title,
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
        resDialogOkToast = R.string.crash_dialog_ok_toast
)
public class CommonsApplication extends Application {
    @Inject SessionManager sessionManager;
    @Inject DBOpenHelper dbOpenHelper;

    @Inject @Named("default_preferences") SharedPreferences defaultPrefs;
    @Inject @Named("application_preferences") SharedPreferences applicationPrefs;
    @Inject @Named("prefs") SharedPreferences otherPrefs;
    @Inject
    @Named("isBeta")
    boolean isBeta;

    /**
     * Constants begin
     */
    public static final int OPEN_APPLICATION_DETAIL_SETTINGS = 1001;

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using [[COM:MOA|Commons Mobile App]]";

    public static final String FEEDBACK_EMAIL = "commons-app-android@googlegroups.com";

    public static final String FEEDBACK_EMAIL_SUBJECT = "Commons Android App (%s) Feedback";

    public static final String NOTIFICATION_CHANNEL_ID_ALL = "CommonsNotificationAll";

    /**
     * Constants End
     */

    private RefWatcher refWatcher;


    /**
     * Used to declare and initialize various components and dependencies
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            //FIXME: Traceur should be disabled for release builds until error fixed
            //See https://github.com/commons-app/apps-android-commons/issues/1877
            Traceur.enableLogging();
        }

        ApplicationlessInjection
                .getInstance(this)
                .getCommonsApplicationComponent()
                .inject(this);

        initTimber();

//        Set DownsampleEnabled to True to downsample the image in case it's heavy
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setDownsampleEnabled(true)
                .build();
        try {
            Fresco.initialize(this, config);
        } catch (Exception e) {
            Timber.e(e);
            // TODO: Remove when we're able to initialize Fresco in test builds.
        }

        // Empty temp directory in case some temp files are created and never removed.
        ContributionUtils.emptyTemporaryDirectory();

        initAcra();
        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
        }

        createNotificationChannel(this);


        if (setupLeakCanary() == RefWatcher.DISABLED) {
            return;
        }
        // Fire progress callbacks for every 3% of uploaded content
        System.setProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "3.0");
    }

    /**
     * Plants debug and file logging tree.
     * Timber lets you plant your own logging trees.
     *
     */
    private void initTimber() {
        String logFileName = isBeta ? "CommonsBetaAppLogs" : "CommonsAppLogs";
        String logDirectory = LogUtils.getLogDirectory(isBeta);
        FileLoggingTree tree = new FileLoggingTree(
                Log.DEBUG,
                logFileName,
                logDirectory,
                1000,
                getFileLoggingThreadPool());

        Timber.plant(tree);
        Timber.plant(new Timber.DebugTree());
    }

    /**
     * Remove ACRA's UncaughtExceptionHandler
     * We do this because ACRA's handler spawns a new process possibly screwing up with a few things
     */
    private void initAcra() {
        Thread.UncaughtExceptionHandler exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        ACRA.init(this);
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
    }

    private ThreadPoolService getFileLoggingThreadPool() {
        return new ThreadPoolService.Builder("file-logging-thread")
                .setPriority(Process.THREAD_PRIORITY_LOWEST)
                .setPoolSize(1)
                .setExceptionHandler(new BackgroundPoolExceptionHandler())
                .build();
    }

    public static void createNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_ALL);
            if (channel == null) {
                channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_ALL,
                        context.getString(R.string.notifications_channel_name_all), NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(channel);
            }
        }
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
    @SuppressLint("CheckResult")
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

        sessionManager.logout()
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
        BookmarkPicturesDao.Table.onDelete(db);
        BookmarkLocationsDao.Table.onDelete(db);
    }

    /**
     * Interface used to get log-out events
     */
    public interface LogoutListener {
        void onLogoutComplete();
    }
}
