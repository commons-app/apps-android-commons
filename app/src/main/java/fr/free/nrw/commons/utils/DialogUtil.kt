package fr.free.nrw.commons.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.view.View
import fr.free.nrw.commons.R
import org.apache.commons.lang3.StringUtils
import timber.log.Timber

object DialogUtil {
    /**
     * Shows a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be shown
     */
    private fun showSafely(activity: Activity, dialog: Dialog) {
        val isActivityDestroyed = activity.isDestroyed
        if (activity.isFinishing || isActivityDestroyed) {
            Timber.e("Activity is not running. Could not show dialog. ")
            return
        }
        try {
            dialog.show()
        } catch (e: IllegalStateException) {
            Timber.e(e, "Could not show dialog.")
        }
    }

    @JvmStatic fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        onPositiveBtnClick: Runnable,
        onNegativeBtnClick: Runnable
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
        onPositiveBtnClick: Runnable,
        onNegativeBtnClick: Runnable
    ) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setMessage(message)
        if (!StringUtils.isBlank(positiveButtonText)) {
            builder.setPositiveButton(
                positiveButtonText
            ) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                onPositiveBtnClick.run()
            }
        }
        if (!StringUtils.isBlank(negativeButtonText)) {
            builder.setNegativeButton(
                negativeButtonText
            ) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                onNegativeBtnClick.run()
            }
        }
        val dialog = builder.create()
        showSafely(activity, dialog)
    }

    /*
    Shows alert dialog with custom view
    */
    @JvmStatic
    fun showAlertDialog(
        activity: Activity, title: String, message: String, onPositiveBtnClick: Runnable,
        onNegativeBtnClick: Runnable,
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

    /*
    Shows alert dialog with custom view
     */
    private fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String,
        onPositiveBtnClick: Runnable,
        onNegativeBtnClick: Runnable,
        customView: View,
        cancelable: Boolean
    ) {
        // If the custom view already has a parent, there is already a dialog showing with the view
        // This happens for on resume - return to avoid creating a second dialog - the first one
        // will still show

        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setView(customView)
        builder.setCancelable(cancelable)
        builder.setPositiveButton(
            positiveButtonText
        ) { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
            onPositiveBtnClick.run()
        }
        builder.setNegativeButton(
            negativeButtonText
        ) { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
            onNegativeBtnClick.run()
        }
        val dialog = builder.create()
        showSafely(activity, dialog)
    }

    /**
     * show a dialog with just a positive button
     * @param activity
     * @param title
     * @param message
     * @param positiveButtonText
     * @param positiveButtonClick
     * @param cancellable
     */
    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String,
        positiveButtonClick: Runnable,
        cancellable: Boolean
    ) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setCancelable(cancellable)
        builder.setPositiveButton(
            positiveButtonText
        ) { dialogInterface: DialogInterface, _: Int ->
            dialogInterface.dismiss()
            positiveButtonClick.run()
        }
        val dialog = builder.create()
        showSafely(activity, dialog)
    }
}