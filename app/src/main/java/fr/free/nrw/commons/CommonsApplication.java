package fr.free.nrw.commons;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraDialog;
import org.acra.annotation.AcraMailSender;
import org.acra.data.StringFormat;
import org.wikipedia.AppAdapter;
import org.wikipedia.language.AppLanguageLookUpTable;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao;
import fr.free.nrw.commons.category.CategoryDao;
import fr.free.nrw.commons.concurrency.BackgroundPoolExceptionHandler;
import fr.free.nrw.commons.concurrency.ThreadPoolService;
import fr.free.nrw.commons.contributions.ContributionDao;
import fr.free.nrw.commons.data.DBOpenHelper;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.logging.FileLoggingTree;
import fr.free.nrw.commons.logging.LogUtils;
import fr.free.nrw.commons.modifications.ModifierSequenceDao;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.ConfigUtils;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.internal.functions.Functions;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.APP_VERSION_NAME;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.STACK_TRACE;
import static org.acra.ReportField.USER_COMMENT;

@AcraCore(
        buildConfigClass = BuildConfig.class,
        resReportSendSuccessToast = R.string.crash_dialog_ok_toast,
        reportFormat = StringFormat.KEY_VALUE_LIST,
        reportContent = {USER_COMMENT, APP_VERSION_CODE, APP_VERSION_NAME, ANDROID_VERSION, PHONE_MODEL, STACK_TRACE}
)

@AcraMailSender(
        mailTo = "commons-app-android-private@googlegroups.com",
        reportAsFile = false
)

@AcraDialog(
        resTheme = R.style.Theme_AppCompat_Dialog,
        resText = R.string.crash_dialog_text,
        resTitle = R.string.crash_dialog_title,
        resCommentPrompt = R.string.crash_dialog_comment_prompt
)

public class CommonsApplication extends Application {
    @Inject SessionManager sessionManager;
    @Inject DBOpenHelper dbOpenHelper;

    @Inject @Named("default_preferences") JsonKvStore defaultPrefs;

    /**
     * Constants begin
     */
    public static final int OPEN_APPLICATION_DETAIL_SETTINGS = 1001;

    public static final String DEFAULT_EDIT_SUMMARY = "Uploaded using [[COM:MOA|Commons Mobile App]]";

    public static final String FEEDBACK_EMAIL = "commons-app-android@googlegroups.com";

    public static final String FEEDBACK_EMAIL_SUBJECT = "Commons Android App Feedback";

    public static final String NOTIFICATION_CHANNEL_ID_ALL = "CommonsNotificationAll";

    public static final String FEEDBACK_EMAIL_TEMPLATE_HEADER = "-- Technical information --";

    /**
     * Constants End
     */

    private RefWatcher refWatcher;

    private static CommonsApplication INSTANCE;
    public static CommonsApplication getInstance() {
        return INSTANCE;
    }

    private AppLanguageLookUpTable languageLookUpTable;
    public AppLanguageLookUpTable getLanguageLookUpTable() {
        return languageLookUpTable;
    }

    /**
     * Used to declare and initialize various components and dependencies
     */
    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
        ACRA.init(this);

        ApplicationlessInjection
                .getInstance(this)
                .getCommonsApplicationComponent()
                .inject(this);

        AppAdapter.set(new CommonsAppAdapter(sessionManager, defaultPrefs));

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

        createNotificationChannel(this);

        languageLookUpTable = new AppLanguageLookUpTable(this);

        // This handler will catch exceptions thrown from Observables after they are disposed,
        // or from Observables that are (deliberately or not) missing an onError handler.
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());

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
        boolean isBeta = ConfigUtils.isBetaFlavour();
        String logFileName = isBeta ? "CommonsBetaAppLogs" : "CommonsAppLogs";
        String logDirectory = LogUtils.getLogDirectory();
        FileLoggingTree tree = new FileLoggingTree(
                Log.DEBUG,
                logFileName,
                logDirectory,
                1000,
                getFileLoggingThreadPool());

        Timber.plant(tree);
        Timber.plant(new Timber.DebugTree());
    }

    public static boolean isRoboUnitTest() {
        return "robolectric".equals(Build.FINGERPRINT);
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

    public String getUserAgent() {
        return "Commons/" + ConfigUtils.getVersionNameWithSha(this) + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE;
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
                    defaultPrefs.clearAll();
                    defaultPrefs.putBoolean("firstrun", false);
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
