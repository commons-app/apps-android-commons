package fr.free.nrw.commons

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable
import fr.free.nrw.commons.category.CategoryDao
import fr.free.nrw.commons.concurrency.BackgroundPoolExceptionHandler
import fr.free.nrw.commons.concurrency.ThreadPoolService
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.ApplicationlessInjection
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.language.AppLanguageLookUpTable
import fr.free.nrw.commons.logging.FileLoggingTree
import fr.free.nrw.commons.logging.LogUtils
import fr.free.nrw.commons.media.CustomOkHttpNetworkFetcher
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.functions.Functions
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.acra.ACRA.init
import org.acra.ReportField
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraMailSender
import org.acra.data.StringFormat
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import javax.inject.Inject
import javax.inject.Named

@AcraCore(
    buildConfigClass = BuildConfig::class,
    resReportSendSuccessToast = R.string.crash_dialog_ok_toast,
    reportFormat = StringFormat.KEY_VALUE_LIST,
    reportContent = [ReportField.USER_COMMENT, ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.STACK_TRACE]
)

@AcraMailSender(mailTo = "commons-app-android-private@googlegroups.com", reportAsFile = false)

@AcraDialog(
    resTheme = R.style.Theme_AppCompat_Dialog,
    resText = R.string.crash_dialog_text,
    resTitle = R.string.crash_dialog_title,
    resCommentPrompt = R.string.crash_dialog_comment_prompt
)

class CommonsApplication : MultiDexApplication() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var dbOpenHelper: DBOpenHelper

    @Inject
    @field:Named("default_preferences")
    lateinit var defaultPrefs: JsonKvStore

    @Inject
    lateinit var cookieJar: CommonsCookieJar

    @Inject
    lateinit var customOkHttpNetworkFetcher: CustomOkHttpNetworkFetcher

    var languageLookUpTable: AppLanguageLookUpTable? = null
        private set

    @Inject
    lateinit var contributionDao: ContributionDao

    /**
     * Used to declare and initialize various components and dependencies
     */
    override fun onCreate() {
        super.onCreate()

        instance = this
        init(this)

        ApplicationlessInjection
            .getInstance(this)
            .commonsApplicationComponent
            .inject(this)

        initTimber()

        if (!defaultPrefs.getBoolean("has_user_manually_removed_location")) {
            var defaultExifTagsSet = defaultPrefs.getStringSet(Prefs.MANAGED_EXIF_TAGS)
            if (null == defaultExifTagsSet) {
                defaultExifTagsSet = HashSet()
            }
            defaultExifTagsSet.add(getString(R.string.exif_tag_location))
            defaultPrefs.putStringSet(Prefs.MANAGED_EXIF_TAGS, defaultExifTagsSet)
        }

        //        Set DownsampleEnabled to True to downsample the image in case it's heavy
        val config = ImagePipelineConfig.newBuilder(this)
            .setNetworkFetcher(customOkHttpNetworkFetcher)
            .setDownsampleEnabled(true)
            .build()
        try {
            Fresco.initialize(this, config)
        } catch (e: Exception) {
            Timber.e(e)
            // TODO: Remove when we're able to initialize Fresco in test builds.
        }

        createNotificationChannel(this)

        languageLookUpTable = AppLanguageLookUpTable(this)

        // This handler will catch exceptions thrown from Observables after they are disposed,
        // or from Observables that are (deliberately or not) missing an onError handler.
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer())

        // Fire progress callbacks for every 3% of uploaded content
        System.setProperty("in.yuvi.http.fluent.PROGRESS_TRIGGER_THRESHOLD", "3.0")
    }

    /**
     * Plants debug and file logging tree. Timber lets you plant your own logging trees.
     */
    private fun initTimber() {
        val isBeta = isBetaFlavour
        val logFileName =
            if (isBeta) "CommonsBetaAppLogs" else "CommonsAppLogs"
        val logDirectory = LogUtils.getLogDirectory()
        //Delete stale logs if they have exceeded the specified size
        deleteStaleLogs(logFileName, logDirectory)

        val tree = FileLoggingTree(
            Log.VERBOSE,
            logFileName,
            logDirectory,
            1000,
            fileLoggingThreadPool
        )

        Timber.plant(tree)
        Timber.plant(DebugTree())
    }

    /**
     * Deletes the logs zip file at the specified directory and file locations specified in the
     * params
     *
     * @param logFileName
     * @param logDirectory
     */
    private fun deleteStaleLogs(logFileName: String, logDirectory: String) {
        try {
            val file = File("$logDirectory/zip/$logFileName.zip")
            if (file.exists() && file.totalSpace > 1000000) { // In Kbs
                file.delete()
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private val fileLoggingThreadPool: ThreadPoolService
        get() = ThreadPoolService.Builder("file-logging-thread")
            .setPriority(Process.THREAD_PRIORITY_LOWEST)
            .setPoolSize(1)
            .setExceptionHandler(BackgroundPoolExceptionHandler())
            .build()

    val userAgent: String
        get() = ("Commons/" + this.getVersionNameWithSha()
                + " (https://mediawiki.org/wiki/Apps/Commons) Android/" + Build.VERSION.RELEASE)

    /**
     * clears data of current application
     *
     * @param context        Application context
     * @param logoutListener Implementation of interface LogoutListener
     */
    @SuppressLint("CheckResult")
    fun clearApplicationData(context: Context, logoutListener: LogoutListener) {
        val cacheDirectory = context.cacheDir
        val applicationDirectory = File(cacheDirectory.parent)
        if (applicationDirectory.exists()) {
            val fileNames = applicationDirectory.list()
            for (fileName in fileNames) {
                if (fileName != "lib") {
                    FileUtils.deleteFile(File(applicationDirectory, fileName))
                }
            }
        }

        sessionManager.logout()
            .andThen(Completable.fromAction { cookieJar.clear() })
            .andThen(Completable.fromAction {
                Timber.d("All accounts have been removed")
                clearImageCache()
                //TODO: fix preference manager
                defaultPrefs.clearAll()
                defaultPrefs.putBoolean("firstrun", false)
                updateAllDatabases()
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ logoutListener.onLogoutComplete() }, { t: Throwable? -> Timber.e(t) })
    }

    /**
     * Clear all images cache held by Fresco
     */
    private fun clearImageCache() {
        val imagePipeline = Fresco.getImagePipeline()
        imagePipeline.clearCaches()
    }

    /**
     * Deletes all tables and re-creates them.
     */
    private fun updateAllDatabases() {
        dbOpenHelper.readableDatabase.close()
        val db = dbOpenHelper.writableDatabase

        CategoryDao.Table.onDelete(db)
        dbOpenHelper.deleteTable(
            db,
            DBOpenHelper.CONTRIBUTIONS_TABLE
        ) //Delete the contributions table in the existing db on older versions

        dbOpenHelper.deleteTable(
            db,
            DBOpenHelper.BOOKMARKS_LOCATIONS
        )

        try {
            contributionDao.deleteAll()
        } catch (e: SQLiteException) {
            Timber.e(e)
        }
        BookmarksTable.onDelete(db)
        BookmarkItemsTable.onDelete(db)
    }


    /**
     * Interface used to get log-out events
     */
    interface LogoutListener {
        fun onLogoutComplete()
    }

    /**
     * This listener is responsible for handling post-logout actions, specifically invoking the LoginActivity
     * with relevant intent parameters. It does not perform the actual logout operation.
     */
    open class BaseLogoutListener : LogoutListener {
        var ctx: Context
        var loginMessage: String? = null
        var userName: String? = null

        /**
         * Constructor for BaseLogoutListener.
         *
         * @param ctx Application context
         */
        constructor(ctx: Context) {
            this.ctx = ctx
        }

        /**
         * Constructor for BaseLogoutListener
         *
         * @param ctx           The application context, used for invoking the LoginActivity and passing relevant intent parameters as part of the post-logout process.
         * @param loginMessage  Message to be displayed on the login page
         * @param loginUsername Username to be pre-filled on the login page
         */
        constructor(
            ctx: Context, loginMessage: String?,
            loginUsername: String?
        ) {
            this.ctx = ctx
            this.loginMessage = loginMessage
            this.userName = loginUsername
        }

        override fun onLogoutComplete() {
            Timber.d("Logout complete callback received.")
            val loginIntent = Intent(ctx, LoginActivity::class.java)
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (loginMessage != null) {
                loginIntent.putExtra(LOGIN_MESSAGE_INTENT_KEY, loginMessage)
            }
            if (userName != null) {
                loginIntent.putExtra(LOGIN_USERNAME_INTENT_KEY, userName)
            }

            ctx.startActivity(loginIntent)
        }
    }

    /**
     * This class is an extension of BaseLogoutListener, providing additional functionality or customization
     * for the logout process. It includes specific actions to be taken during logout, such as handling redirection to the login screen.
     */
    class ActivityLogoutListener : BaseLogoutListener {
        var activity: Activity


        /**
         * Constructor for ActivityLogoutListener.
         *
         * @param activity The activity context from which the logout is initiated. Used to perform actions such as finishing the activity.
         * @param ctx           The application context, used for invoking the LoginActivity and passing relevant intent parameters as part of the post-logout process.
         */
        constructor(activity: Activity, ctx: Context) : super(ctx) {
            this.activity = activity
        }

        /**
         * Constructor for ActivityLogoutListener with additional parameters for the login screen.
         *
         * @param activity      The activity context from which the logout is initiated. Used to perform actions such as finishing the activity.
         * @param ctx           The application context, used for invoking the LoginActivity and passing relevant intent parameters as part of the post-logout process.
         * @param loginMessage  Message to be displayed on the login page after logout.
         * @param loginUsername Username to be pre-filled on the login page after logout.
         */
        constructor(
            activity: Activity, ctx: Context?,
            loginMessage: String?, loginUsername: String?
        ) : super(activity, loginMessage, loginUsername) {
            this.activity = activity
        }

        override fun onLogoutComplete() {
            super.onLogoutComplete()
            activity.finish()
        }
    }

    companion object {

        const val LOGIN_MESSAGE_INTENT_KEY: String = "loginMessage"
        const val LOGIN_USERNAME_INTENT_KEY: String = "loginUsername"

        const val IS_LIMITED_CONNECTION_MODE_ENABLED: String = "is_limited_connection_mode_enabled"

        /**
         * Constants begin
         */
        const val OPEN_APPLICATION_DETAIL_SETTINGS: Int = 1001

        const val DEFAULT_EDIT_SUMMARY: String = "Uploaded using [[COM:MOA|Commons Mobile App]]"

        const val FEEDBACK_EMAIL: String = "commons-app-android@googlegroups.com"

        const val FEEDBACK_EMAIL_SUBJECT: String = "Commons Android App Feedback"

        const val REPORT_EMAIL: String = "commons-app-android-private@googlegroups.com"

        const val REPORT_EMAIL_SUBJECT: String = "Report a violation"

        const val NOTIFICATION_CHANNEL_ID_ALL: String = "CommonsNotificationAll"

        const val FEEDBACK_EMAIL_TEMPLATE_HEADER: String = "-- Technical information --"

        /**
         * Constants End
         */

        @JvmStatic
        lateinit var instance: CommonsApplication
            private set

        @JvmField
        var isPaused: Boolean = false

        @JvmStatic
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context
                    .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                var channel = manager
                    .getNotificationChannel(NOTIFICATION_CHANNEL_ID_ALL)
                if (channel == null) {
                    channel = NotificationChannel(
                        NOTIFICATION_CHANNEL_ID_ALL,
                        context.getString(R.string.notifications_channel_name_all),
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }
}

