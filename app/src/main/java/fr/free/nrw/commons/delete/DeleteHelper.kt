package fr.free.nrw.commons.delete

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.notification.NotificationHelper
import fr.free.nrw.commons.notification.NotificationHelper.Companion.NOTIFICATION_DELETE
import fr.free.nrw.commons.review.ReviewController
import fr.free.nrw.commons.utils.LangCodeUtils.getLocalizedResources
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Refactored async task to Rx
 */
@Singleton
class DeleteHelper @Inject constructor(
    private val notificationHelper: NotificationHelper,
    @Named("commons-page-edit") private val pageEditClient: PageEditClient,
    private val viewUtil: ViewUtilWrapper,
    @Named("username") private val username: String
) {
    private var d: AlertDialog? = null
    private var listener: DialogInterface.OnMultiChoiceClickListener? = null

    /**
     * Public interface to nominate a particular media file for deletion
     * @param context
     * @param media
     * @param reason
     * @return
     */
    fun makeDeletion(
        context: Context?,
        media: Media?,
        reason: String?
    ): Single<Boolean>? {
        if(context == null && media == null) {
            return null
        }

        viewUtil.showShortToast(
            context!!,
            "Trying to nominate ${media?.displayTitle} for deletion"
        )

        return reason?.let {
            delete(media!!, it)
                .flatMapSingle { result ->
                    Single.just(showDeletionNotification(context, media, result))
                }
                .firstOrError()
                .onErrorResumeNext { throwable ->
                    if (throwable is InvalidLoginTokenException) {
                        Single.error(throwable)
                    } else {
                        Single.error(throwable)
                    }
                }
        }
    }

    /**
     * Makes several API calls to nominate the file for deletion
     * @param media
     * @param reason
     * @return
     */
    private fun delete(media: Media, reason: String): Observable<Boolean> {
        val summary = "Nominating ${media.filename} for deletion."
        val calendar = Calendar.getInstance()
        val fileDeleteString = """
            {{delete|reason=$reason|subpage=${media.filename}|day=
            ${calendar.get(Calendar.DAY_OF_MONTH)}|month=${
            calendar.getDisplayName(
                Calendar.MONTH,
                Calendar.LONG,
                Locale.ENGLISH
            )
        }|year=${calendar.get(Calendar.YEAR)}}}
        """.trimIndent()

        val subpageString = """
            === [[:${media.filename}]] ===
            $reason ~~~~
        """.trimIndent()

        val logPageString = "\n{{Commons:Deletion requests/${media.filename}}}\n"
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val date = sdf.format(calendar.time)

        val userPageString = "\n{{subst:idw|${media.filename}}} ~~~~"

        val creator = media.getAuthorOrUser()
            ?: throw RuntimeException("Failed to nominate for deletion")

        return pageEditClient.prependEdit(
            media.filename!!,
            "$fileDeleteString\n",
            summary
        )
            .onErrorResumeNext { throwable: Throwable ->
                if (throwable is InvalidLoginTokenException) {
                    Observable.error(throwable)
                } else {
                    Observable.error(throwable)
                }
            }
            .flatMap { result: Boolean ->
                if (result) {
                    pageEditClient.edit(
                        "Commons:Deletion_requests/${media.filename}",
                        "$subpageString\n",
                        summary
                    )
                } else {
                    Observable.error(RuntimeException("Failed to nominate for deletion"))
                }
            }
            .flatMap { result: Boolean ->
                if (result) {
                    pageEditClient.appendEdit(
                        "Commons:Deletion_requests/$date",
                        "$logPageString\n",
                        summary
                    )
                } else {
                    Observable.error(RuntimeException("Failed to nominate for deletion"))
                }
            }
            .flatMap { result: Boolean ->
                if (result) {
                    pageEditClient.appendEdit("User_Talk:$creator", "$userPageString\n", summary)
                } else {
                    Observable.error(RuntimeException("Failed to nominate for deletion"))
                }
            }
    }

    @SuppressLint("StringFormatInvalid")
    private fun showDeletionNotification(
        context: Context,
        media: Media,
        result: Boolean
    ): Boolean {
        val title: String
        val message: String
        var baseTitle = context.getString(R.string.delete_helper_show_deletion_title)

        if (result) {
            baseTitle += ": ${
                context.getString(R.string.delete_helper_show_deletion_title_success)
            }"
            title = baseTitle
            message = context
                .getString(R.string.delete_helper_show_deletion_message_if, media.displayTitle)
        } else {
            baseTitle += ": ${context.getString(R.string.delete_helper_show_deletion_title_failed)}"
            title = baseTitle
            message = context.getString(R.string.delete_helper_show_deletion_message_else)
        }

        val urlForDelete = "${BuildConfig.COMMONS_URL}/wiki/Commons:Deletion_requests/${
            media.filename
        }"
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlForDelete))
        notificationHelper
            .showNotification(context, title, message, NOTIFICATION_DELETE, browserIntent)
        return result
    }

    /**
     * Invoked when a reason needs to be asked before nominating for deletion
     * @param media
     * @param context
     * @param question
     * @param problem
     */
    @SuppressLint("CheckResult")
    fun askReasonAndExecute(
        media: Media?,
        context: Context,
        question: String,
        problem: ReviewController.DeleteReason,
        reviewCallback: ReviewController.ReviewCallback
    ) {
        val alert = AlertDialog.Builder(context)
        alert.setCancelable(false)
        alert.setTitle(question)

        val checkedItems = booleanArrayOf(false, false, false, false)
        val mUserReason = arrayListOf<Int>()

        val reasonList: Array<String>
        val reasonListEnglish: Array<String>

        when (problem) {
            ReviewController.DeleteReason.SPAM -> {
                reasonList = arrayOf(
                    context.getString(R.string.delete_helper_ask_spam_selfie),
                    context.getString(R.string.delete_helper_ask_spam_blurry),
                    context.getString(R.string.delete_helper_ask_spam_nonsense)
                )
                reasonListEnglish = arrayOf(
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(R.string.delete_helper_ask_spam_selfie),
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(R.string.delete_helper_ask_spam_blurry),
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(R.string.delete_helper_ask_spam_nonsense)
                )
            }
            ReviewController.DeleteReason.COPYRIGHT_VIOLATION -> {
                reasonList = arrayOf(
                    context.getString(R.string.delete_helper_ask_reason_copyright_press_photo),
                    context.getString(R.string.delete_helper_ask_reason_copyright_internet_photo),
                    context.getString(R.string.delete_helper_ask_reason_copyright_logo),
                    context.getString(
                        R.string.delete_helper_ask_reason_copyright_no_freedom_of_panorama
                    )
                )
                reasonListEnglish = arrayOf(
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(R.string.delete_helper_ask_reason_copyright_press_photo),
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(R.string.delete_helper_ask_reason_copyright_internet_photo),
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(R.string.delete_helper_ask_reason_copyright_logo),
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(
                            R.string.delete_helper_ask_reason_copyright_no_freedom_of_panorama
                        )
                )
            }
            else -> {
                reasonList = emptyArray()
                reasonListEnglish = emptyArray()
            }
        }

        alert.setMultiChoiceItems(
            reasonList,
            checkedItems
        ) { dialogInterface, position, isChecked ->
            if (isChecked) {
                mUserReason.add(position)
            } else {
                mUserReason.remove(position)
            }

            // Safely enable or disable the OK button based on selection
            val dialog = dialogInterface as? AlertDialog
            dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = mUserReason.isNotEmpty()
        }

        alert.setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            reviewCallback.disableButtons()

            val reason = buildString {
                append(
                    getLocalizedResources(context, Locale.ENGLISH)
                        .getString(R.string.delete_helper_ask_alert_set_positive_button_reason)
                )
                append(" ")

                mUserReason.forEachIndexed { index, position ->
                    append(reasonListEnglish[position])
                    if (index != mUserReason.lastIndex) {
                        append(", ")
                    }
                }
            }

            Timber.d("thread is askReasonAndExecute %s", Thread.currentThread().name)

            if (media != null) {
                Single.defer { makeDeletion(context, media, reason) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ reviewCallback.onSuccess() }, { throwable ->
                        when (throwable) {
                            is InvalidLoginTokenException ->
                                reviewCallback.onTokenException(throwable)
                            else -> reviewCallback.onFailure()
                        }
                        reviewCallback.enableButtons()
                    })
            }
        }
        alert.setNegativeButton(
            context.getString(R.string.cancel)
        ) { _, _ -> reviewCallback.onFailure() }

        d = alert.create()
        d?.setOnShowListener {
            // Safely initialize the OK button state after the dialog is fully shown
            d?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = false
        }
        d?.show()
    }


    /**
     * returns the instance of shown AlertDialog,
     * used for taking reference during unit test
     */
    fun getDialog(): AlertDialog? = d

    /**
     * returns the instance of shown DialogInterface.OnMultiChoiceClickListener,
     * used for taking reference during unit test
     */
    fun getListener(): DialogInterface.OnMultiChoiceClickListener? = listener
}