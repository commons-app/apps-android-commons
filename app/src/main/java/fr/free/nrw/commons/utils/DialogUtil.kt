package fr.free.nrw.commons.utils

import android.app.Activity
import android.app.AlertDialog
import android.view.View
import fr.free.nrw.commons.R
import timber.log.Timber

data class DialogOptions(
    val title: String? = null,
    val message: String? = null,
    val positiveButtonText: String? = null,
    val negativeButtonText: String? = null,
    val onPositiveBtnClick: Runnable? = null,
    val onNegativeBtnClick: Runnable? = null,
    val customView: View? = null,
    val cancelable: Boolean = false,
)

object DialogUtil {
    /**
     * Shows a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be shown
     */
    private fun showSafely(
        activity: Activity?,
        dialog: AlertDialog?,
    ): AlertDialog? {
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
        options: DialogOptions,
    ): AlertDialog? = createAndShowDialogSafely(activity, options)

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
    ): AlertDialog? =
        showAlertDialog(
            activity = activity,
            options =
                DialogOptions(
                    title = title,
                    message = message,
                    positiveButtonText = activity.getString(R.string.yes),
                    negativeButtonText = activity.getString(R.string.no),
                    onPositiveBtnClick = onPositiveBtnClick,
                    onNegativeBtnClick = onNegativeBtnClick,
                ),
        )

    @JvmStatic
    @Suppress("LongParameterList")
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        positiveButtonText: String?,
        negativeButtonText: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
        cancelable: Boolean = false,
    ): AlertDialog? =
        showAlertDialog(
            activity = activity,
            options =
                DialogOptions(
                    title = title,
                    message = message,
                    positiveButtonText = positiveButtonText,
                    negativeButtonText = negativeButtonText,
                    onPositiveBtnClick = onPositiveBtnClick,
                    onNegativeBtnClick = onNegativeBtnClick,
                    cancelable = cancelable,
                ),
        )

    @JvmStatic
    @Suppress("LongParameterList")
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
        customView: View?,
    ): AlertDialog? =
        showAlertDialog(
            activity = activity,
            options =
                DialogOptions(
                    title = title,
                    message = message,
                    positiveButtonText = activity.getString(R.string.yes),
                    negativeButtonText = activity.getString(R.string.no),
                    onPositiveBtnClick = onPositiveBtnClick,
                    onNegativeBtnClick = onNegativeBtnClick,
                    customView = customView,
                ),
        )

    @JvmStatic
    @Suppress("LongParameterList")
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        positiveButtonText: String?,
        negativeButtonText: String?,
        onPositiveBtnClick: Runnable?,
        onNegativeBtnClick: Runnable?,
        customView: View?,
    ): AlertDialog? =
        showAlertDialog(
            activity = activity,
            options =
                DialogOptions(
                    title = title,
                    message = message,
                    positiveButtonText = positiveButtonText,
                    negativeButtonText = negativeButtonText,
                    onPositiveBtnClick = onPositiveBtnClick,
                    onNegativeBtnClick = onNegativeBtnClick,
                    customView = customView,
                ),
        )

    @JvmStatic
    fun showAlertDialog(
        activity: Activity,
        title: String?,
        message: String?,
        positiveButtonText: String?,
        onPositiveBtnClick: Runnable?,
    ): AlertDialog? =
        showAlertDialog(
            activity = activity,
            options =
                DialogOptions(
                    title = title,
                    message = message,
                    positiveButtonText = positiveButtonText,
                    onPositiveBtnClick = onPositiveBtnClick,
                ),
        )

    /**
     * show a dialog safely using DialogOptions
     */
    private fun createAndShowDialogSafely(
        activity: Activity,
        options: DialogOptions,
    ): AlertDialog? {
        if (options.customView?.parent != null) {
            return null
        }

        return showSafely(
            activity,
            AlertDialog
                .Builder(activity)
                .apply {
                    options.title?.also { setTitle(it) }
                    options.message?.also { setMessage(it) }
                    setView(options.customView)
                    setCancelable(options.cancelable)
                    options.positiveButtonText?.let {
                        setPositiveButton(it) { _, _ -> options.onPositiveBtnClick?.run() }
                    }
                    options.negativeButtonText?.let {
                        setNegativeButton(it) { _, _ -> options.onNegativeBtnClick?.run() }
                    }
                }.create(),
        )
    }
}
