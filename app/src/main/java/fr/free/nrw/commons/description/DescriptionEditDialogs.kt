package fr.free.nrw.commons.description

import android.app.Activity
import android.app.ProgressDialog
import fr.free.nrw.commons.R
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog

/** Owns the informational and progress dialogs for editing descriptions. */
internal class DescriptionEditDialogs(
    private val activity: Activity,
) {
    private var progressDialog: ProgressDialog? = null

    /**
     * show dialog with info
     * @param titleStringID Title ID
     * @param messageStringId Message ID
     */
    fun showInfoAlert(
        titleStringID: Int,
        messageStringId: Int,
    ) {
        showAlertDialog(
            activity,
            activity.getString(titleStringID),
            activity.getString(messageStringId),
            activity.getString(R.string.ok),
            null,
        )
    }

    /** Shows progress while descriptions and captions are being updated. */
    fun showLoggingProgressBar() {
        progressDialog = ProgressDialog(activity)
        progressDialog!!.isIndeterminate = true
        progressDialog!!.setTitle(activity.getString(R.string.updating_caption_title))
        progressDialog!!.setMessage(activity.getString(R.string.updating_caption_message))
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()
    }
}
