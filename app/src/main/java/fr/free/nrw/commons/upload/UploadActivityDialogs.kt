package fr.free.nrw.commons.upload

import android.content.DialogInterface
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaPresenter
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import timber.log.Timber

/** Displays upload progress, account prompts, and confirmation dialogs. */
internal class UploadActivityDialogs : UploadContract.Feedback {
    lateinit var view: UploadActivityView
    private val activity get() = view.activity

    /** Reports whether the current session is authenticated. */
    override fun isLoggedIn(): Boolean = activity.sessionManager!!.isUserLoggedIn

    /** Closes the upload flow. */
    override fun returnToMainActivity() {
        activity.finish()
    }

    /**
     * go to the uploadProgress activity to check the status of uploading
     */
    override fun goToUploadProgressActivity() {
        activity.startActivity(Intent(activity, UploadProgressActivity::class.java))
    }

    /**
     * Show/Hide the progress dialog
     */
    override fun showProgress(shouldShow: Boolean) {
        if (shouldShow) {
            if (!view.progressDialog!!.isShowing) {
                view.progressDialog!!.show()
            }
        } else {
            if (view.progressDialog != null && !activity.isFinishing) {
                view.progressDialog!!.dismiss()
            }
        }
    }

    /** Displays an upload status message. */
    override fun showMessage(messageResourceId: Int) {
        showLongToast(activity, messageResourceId)
    }

    /** Prompts an unauthenticated user to sign in. */
    override fun askUserToLogIn() {
        Timber.d("current session is null, asking user to login")
        showLongToast(activity, activity.getString(R.string.user_not_logged_in))
        val loginIntent = Intent(activity, LoginActivity::class.java)
        activity.startActivity(loginIntent)
    }

    /**
     * Used to check if user has cancelled upload of any image in current upload
     * so that location compare doesn't show up again in same upload.
     * Fixes: [Issue](https://github.com/commons-app/apps-android-commons/issues/5511)
     *
     * @param isCancelled Is true when user has cancelled upload of any image in current upload
     */
    override fun setImageCancelled(isCancelled: Boolean) {
        val basicKvStore = BasicKvStore(activity, "IsAnyImageCancelled")
        basicKvStore.putBoolean("IsAnyImageCancelled", isCancelled)
    }

    /** Displays an upload alert and runs its acknowledgement action. */
    override fun showAlertDialog(
        messageResourceId: Int,
        onPositiveClick: Runnable,
    ) {
        showAlertDialog(
            activity,
            "",
            activity.getString(messageResourceId),
            activity.getString(R.string.ok),
            onPositiveClick,
        )
    }

    /**
     * If the user uploads more than 1 file informs that
     * depictions/categories apply to all pictures of a multi upload.
     * This method takes no arguments and does not return any value.
     * It shows the AlertDialog and continues the flow of uploads.
     */
    fun showAlertDialogForCategories() {
        UploadMediaPresenter.isCategoriesDialogShowing = true
        // Inflate the custom layout
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.activity_upload_categories_dialog, null)
        val checkBox = view.findViewById<CheckBox>(R.id.categories_checkbox)
        // Create the alert dialog
        val alertDialog =
            AlertDialog
                .Builder(activity)
                .setView(view)
                .setTitle(activity.getString(R.string.multiple_files_depiction_header))
                .setMessage(activity.getString(R.string.multiple_files_depiction))
                .setPositiveButton("OK") { _: DialogInterface?, _: Int ->
                    if (checkBox.isChecked) {
                        // Save the user's choice to not show the dialog again
                        activity.defaultKvStore.putBoolean("hasAlreadyLaunchedCategoriesDialog", true)
                    }
                    activity.presenter!!.checkImageQuality(0)
                    UploadMediaPresenter.isCategoriesDialogShowing = false
                }.setNegativeButton("", null)
                .create()
        alertDialog.show()
    }

    /** Suggest users to turn battery optimisation off when uploading
     * more than a few files. That's because we have noticed that
     * many-files uploads have a much higher probability of failing
     * than uploads with less files. Show the dialog for Android 6
     * and above as the ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
     * intent was added in API level 23
     */
    fun showAlertForBattery() {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            // When battery-optimisation dialog is shown don't show the image quality dialog
            UploadMediaPresenter.isBatteryDialogShowing = true
            showAlertDialog(
                activity,
                activity.getString(R.string.unrestricted_battery_mode),
                activity.getString(R.string.suggest_unrestricted_mode),
                activity.getString(R.string.title_activity_settings),
                activity.getString(R.string.cancel),
                {
                    /* Since opening the right settings page might be device dependent, using
                      https://github.com/WaseemSabir/BatteryPermissionHelper
                      directly appeared like a promising idea.
                      However, this simply closed the popup and did not make
                      the settings page appear on a Pixel as well as a Xiaomi device.
                      Used the standard intent instead of using this library as
                      it shows a list of all the apps on the device and allows users to
                      turn battery optimisation off.
                     */
                    val batteryOptimisationSettingsIntent =
                        Intent(
                            Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                        )
                    activity.startActivity(batteryOptimisationSettingsIntent)

                    // calling checkImageQuality after battery dialog is interacted with
                    // so that 2 dialogs do not pop up simultaneously
                    UploadMediaPresenter.isBatteryDialogShowing = false
                },
                {
                    UploadMediaPresenter.isBatteryDialogShowing = false
                },
            )
            activity.defaultKvStore.putBoolean("hasAlreadyLaunchedBigMultiupload", true)
        }
    }
}
