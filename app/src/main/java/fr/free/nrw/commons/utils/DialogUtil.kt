package fr.free.nrw.commons.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.view.View
import fr.free.nrw.commons.R
import timber.log.Timber

object DialogUtil {
    /**
     * Shows a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be shown
     */
    private fun showSafely(activity: Activity?, dialog: Dialog?) {

        if (activity == null || dialog == null) {
            Timber.d("Show called with null activity / dialog. Ignoring.")
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            Timber.e("Activity is not running. Could not show dialog. ")
            return
        }
        try {
            dialog.show()
        } catch (e: IllegalStateException) {
            Timber.e(e, "Could not show dialog.")
        }
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?
    ) {
        showAlertDialog(
            activity,
            title,
            message,
            activity.getString(R.string.yes),
            activity.getString(R.string.no),
            onPositiveBtnClick,
            onNegativeBtnClick
        )
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?
    ) {
        showAlertDialog(
            activity,
            title,
            message,
            positiveButtonText,
            negativeButtonText,
            onPositiveBtnClick,
            onNegativeBtnClick
        )
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
        customView: View,
        cancelable: Boolean
    ) {
        showAlertDialog(
            activity,
            title,
            message,
            activity.getString(R.string.yes),
            activity.getString(R.string.no),
            onPositiveBtnClick,
            onNegativeBtnClick,
            customView,
            cancelable
        )
    }

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String?,
        onPositiveBtnClick: Runnable?,
        cancellable: Boolean
    ) {
        showAlertDialog(
            activity,
            title,
            message,
            positiveButtonText,
            onPositiveBtnClick,
            cancellable
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
    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String? = let { "" },
        negativeButtonText: String? = let { "" },
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
        customView: View? = let { null },
        cancelable: Boolean? = let { false }
    ) {

        /* If the custom view already has a parent, there is already a dialog showing with the view
         * This happens for on resume - return to avoid creating a second dialog - the first one
         * will still show
         */
        if (customView?.parent != null) {
            return
        }

        showSafely(activity, AlertDialog.Builder(activity).apply {
            setTitle(title)
            setMessage(message)
            setView(customView)
            cancelable?.let { setCancelable(it) }
            positiveButtonText?.let {
                if (positiveButtonText.isBlank()) {
                    setPositiveButton(positiveButtonText) { _: DialogInterface, _: Int ->
                        onPositiveBtnClick?.run()
                    }
                }
            }
            negativeButtonText?.let {
                if (negativeButtonText.isBlank()) {
                    setNegativeButton(negativeButtonText) { _: DialogInterface, _: Int ->
                        onNegativeBtnClick?.run()
                    }
                }
            }
        }.create())
    }
}