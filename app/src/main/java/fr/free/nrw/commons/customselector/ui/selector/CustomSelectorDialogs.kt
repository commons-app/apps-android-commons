package fr.free.nrw.commons.customselector.ui.selector

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.Button
import android.widget.TextView
import fr.free.nrw.commons.R

/**
 * Shows the dialogs of the custom selector.
 */
object CustomSelectorDialogs {
    /**
     * Show Custom Selector Welcome Dialog.
     *
     * @param context The context used to build the dialog.
     */
    fun showWelcomeDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.setCancelable(false)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_selector_info_dialog)
        (dialog.findViewById<Button>(R.id.btn_ok))?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Displays a dialog explaining the upload limit warning.
     *
     * @param context The context used to build the dialog.
     * @param uploadLimit Maximum number of images that can be selected.
     * @param uploadLimitExceededBy The amount by which the upload limit has been exceeded.
     */
    fun showUploadLimitWarning(
        context: Context,
        uploadLimit: Int,
        uploadLimitExceededBy: Int,
    ) {
        val dialog = Dialog(context)
        dialog.setCancelable(false)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_selector_limit_dialog)
        (dialog.findViewById<Button>(R.id.btn_dismiss_limit_warning))?.setOnClickListener { dialog.dismiss() }
        (dialog.findViewById<TextView>(R.id.upload_limit_warning))?.text =
            context.resources.getString(
                R.string.custom_selector_over_limit_warning,
                uploadLimit,
                uploadLimitExceededBy,
            )
        dialog.show()
    }

    /**
     * Warns users when duplicate-content images are selected before finishing selection.
     *
     * @param context The context used to build the dialog.
     * @param onConfirm Called when the user confirms to continue with the unique images.
     */
    fun showDuplicateSelectionWarning(
        context: Context,
        onConfirm: () -> Unit,
    ) {
        AlertDialog
            .Builder(context)
            .setTitle(R.string.warning)
            .setMessage(R.string.custom_selector_duplicates_warning)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ -> onConfirm() }
            .show()
    }
}
