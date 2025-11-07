package fr.free.nrw.commons.review

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context

import androidx.core.app.NotificationCompat

import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage

import java.util.ArrayList

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.actions.ThanksClient
import fr.free.nrw.commons.delete.DeleteHelper
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


@Singleton
class ReviewController @Inject constructor(
    private val deleteHelper: DeleteHelper,
    context: Context
) {

    companion object {
        private const val NOTIFICATION_SEND_THANK = 0x102
        private const val NOTIFICATION_CHECK_CATEGORY = 0x101
        protected var categories: ArrayList<String> = ArrayList()
    }

    @Inject
    lateinit var thanksClient: ThanksClient

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    @field: Named("commons-page-edit")
    lateinit var pageEditClient: PageEditClient

    var firstRevision: MwQueryPage.Revision? = null // TODO: maybe we can expand this class to include fileName

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(context, CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL)

    var media: Media? = null

    init {
        CommonsApplication.createNotificationChannel(context.applicationContext)
    }

    fun onImageRefreshed(media: Media) {
        this.media = media
    }

    enum class DeleteReason {
        SPAM,
        COPYRIGHT_VIOLATION
    }

    fun reportSpam(activity: Activity, reviewCallback: ReviewCallback) {
        Timber.d("Report spam for %s", media?.filename)
        deleteHelper.askReasonAndExecute(
            media,
            activity,
            activity.resources.getString(R.string.review_spam_report_question),
            DeleteReason.SPAM,
            reviewCallback
        )
    }

    fun reportPossibleCopyRightViolation(activity: Activity, reviewCallback: ReviewCallback) {
        Timber.d("Report copyright violation for %s", media?.filename)
        deleteHelper.askReasonAndExecute(
            media,
            activity,
            activity.resources.getString(R.string.review_c_violation_report_question),
            DeleteReason.COPYRIGHT_VIOLATION,
            reviewCallback
        )
    }

    @SuppressLint("CheckResult")
    fun reportWrongCategory(activity: Activity, reviewCallback: ReviewCallback) {
        val context = activity.applicationContext
        // Dependencies are already injected via @Inject annotations in the class

        ViewUtil.showShortToast(
            context,
            context.getString(R.string.check_category_toast, media?.displayTitle)
        )

        publishProgress(context, 0)
        val summary = context.getString(R.string.check_category_edit_summary)

        Observable.defer {
            pageEditClient.appendEdit(media?.filename ?: "", "\n{{subst:chc}}\n", summary)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                publishProgress(context, 2)
                val (title, message) = if (result) {
                    reviewCallback.onSuccess()
                    context.getString(R.string.check_category_success_title) to
                            context.getString(R.string.check_category_success_message, media?.displayTitle)
                } else {
                    reviewCallback.onFailure()
                    context.getString(R.string.check_category_failure_title) to
                            context.getString(R.string.check_category_failure_message, media?.displayTitle)
                }
                showNotification(title, message)
            }, Timber::e)
    }

    private fun publishProgress(context: Context, progress: Int) {
        val messages = arrayOf(
            R.string.getting_edit_token,
            R.string.check_category_adding_template
        )

        val message = if (progress in 1 until messages.size) {
            context.getString(messages[progress])
        } else ""

        notificationBuilder.setContentTitle(
            context.getString(
                R.string.check_category_notification_title,
                media?.displayTitle
            )
        )
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_launcher)
            .setProgress(messages.size, progress, false)
            .setOngoing(true)

        notificationManager.notify(NOTIFICATION_CHECK_CATEGORY, notificationBuilder.build())
    }

    @SuppressLint("CheckResult")
    fun sendThanks(activity: Activity) {
        val context = activity.applicationContext
        // Dependencies are already injected via @Inject annotations in the class

        ViewUtil.showShortToast(
            context,
            context.getString(R.string.send_thank_toast, media?.displayTitle)
        )

        if (firstRevision == null) return

        Observable.defer {
            thanksClient.thank(firstRevision!!.revisionId())
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ result ->
                displayThanksToast(context, result)
            }, { throwable ->
                if (throwable is InvalidLoginTokenException) {
                    val username = sessionManager.userName
                    val logoutListener = CommonsApplication.BaseLogoutListener(
                        activity,
                        activity.getString(R.string.invalid_login_message),
                        username
                    )
                    CommonsApplication.instance.clearApplicationData(activity, logoutListener)
                } else {
                    Timber.e(throwable)
                }
            })
    }

    @SuppressLint("StringFormatInvalid")
    private fun displayThanksToast(context: Context, result: Boolean) {
        val (title, message) = if (result) {
            context.getString(R.string.send_thank_success_title) to
                    context.getString(R.string.send_thank_success_message, media?.displayTitle)
        } else {
            context.getString(R.string.send_thank_failure_title) to
                    context.getString(R.string.send_thank_failure_message, media?.displayTitle)
        }

        ViewUtil.showShortToast(context, message)
    }

    private fun showNotification(title: String, message: String) {
        notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_launcher)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        notificationManager.notify(NOTIFICATION_SEND_THANK, notificationBuilder.build())
    }

    interface ReviewCallback {
        fun onSuccess()
        fun onFailure()
        fun onTokenException(e: Exception)
        fun disableButtons()
        fun enableButtons()
    }
}
