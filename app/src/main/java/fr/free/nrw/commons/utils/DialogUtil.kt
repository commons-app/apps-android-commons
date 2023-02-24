package fr.free.nrw.commons.utils

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import fr.free.nrw.commons.R
import timber.log.Timber

object DialogUtil {
    /**
     * Shows a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be shown
     */
    private fun showSafely(activity: Activity?, dialog: AlertDialog?):AlertDialog? {

        if (activity == null || dialog == null) {
            Timber.d("Show called with null activity / dialog. Ignoring.")
            return null
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Timber.e("Activity is not running. Could not show dialog. ")
            return dialog
        }
        try {
            dialog.show()
        } catch (e: IllegalStateException) {
            Timber.e(e, "Could not show dialog.")
        }
        return dialog
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?
    ): AlertDialog? {
        return createAndShowDialogSafely(
            activity = activity,
            title = title,
            message = message,
            positiveButtonText = activity.getString(R.string.yes),
            negativeButtonText = activity.getString(R.string.no),
            onPositiveBtnClick = onPositiveBtnClick,
            onNegativeBtnClick = onNegativeBtnClick
        )
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        positiveButtonText: String?,
        negativeButtonText: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
    ): AlertDialog? {
        return createAndShowDialogSafely(
            activity = activity,
            title = title,
            message = message,
            positiveButtonText = positiveButtonText,
            negativeButtonText = negativeButtonText,
            onPositiveBtnClick = onPositiveBtnClick,
            onNegativeBtnClick = onNegativeBtnClick
        )
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
        customView: View?,
        cancelable: Boolean
    ): AlertDialog? {
        return createAndShowDialogSafely(
            activity = activity,
            title = title,
            message = message,
            positiveButtonText = activity.getString(R.string.yes),
            negativeButtonText = activity.getString(R.string.no),
            onPositiveBtnClick = onPositiveBtnClick,
            onNegativeBtnClick = onNegativeBtnClick,
            customView = customView,
            cancelable = cancelable
        )
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        positiveButtonText: String?,
        negativeButtonText: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
        customView: View?,
        cancelable: Boolean
    ): AlertDialog? {
        return createAndShowDialogSafely(
            activity = activity,
            title = title,
            message = message,
            positiveButtonText = positiveButtonText,
            negativeButtonText = negativeButtonText,
            onPositiveBtnClick = onPositiveBtnClick,
            onNegativeBtnClick = onNegativeBtnClick,
            customView = customView,
            cancelable = cancelable
        )
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        positiveButtonText: String?,
        onPositiveBtnClick: Runnable?,
        cancelable: Boolean
    ): AlertDialog? {
        return createAndShowDialogSafely(
            activity = activity,
            title = title,
            message = message,
            positiveButtonText = positiveButtonText,
            onPositiveBtnClick = onPositiveBtnClick,
            cancelable = cancelable
        )
    }

    /**
     * show a dialog
     * @param activity
     * @param title
     * @param message
     * @param positiveButtonText
     * @param negativeButtonText
     * @param onPositiveBtnClick
     * @param onNegativeBtnClick
     * @param customView
     * @param cancelable
     */
    private fun createAndShowDialogSafely(
        activity: Activity,
        title: String?,
        message: String?,
        positiveButtonText: String? = null,
        negativeButtonText: String? = null,
        onPositiveBtnClick: Runnable? = null,
        onNegativeBtnClick: Runnable? = null,
        customView: View? = null,
        cancelable: Boolean = true
    ): AlertDialog? {

        /* If the custom view already has a parent, there is already a dialog showing with the view
         * This happens for on resume - return to avoid creating a second dialog - the first one
         * will still show
         */
        if (customView?.parent != null) {
            return null
        }

        return showSafely(activity, AlertDialog.Builder(activity).apply {
            title?.also{setTitle(title)}
            message?.also{setMessage(message)}
            setView(customView)
            setCancelable(cancelable)
            positiveButtonText?.let {
                setPositiveButton(it) { _, _ -> onPositiveBtnClick?.run() }
            }
            negativeButtonText?.let {
                setNegativeButton(it) { _, _ -> onNegativeBtnClick?.run() }
            }
        }.create())
    }
}
