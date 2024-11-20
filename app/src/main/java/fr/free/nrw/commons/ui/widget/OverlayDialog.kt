package fr.free.nrw.commons.ui.widget

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager

import androidx.fragment.app.DialogFragment

/**
 * A formatted dialog fragment
 * This class is used by NearbyInfoDialog
 */
abstract class OverlayDialog : DialogFragment() {

    /**
     * Creates a DialogFragment with the correct style and theme
     * @param savedInstanceState bundle re-constructed from a previous saved state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light)
    }

    /**
     * When the view is created, sets the dialog layout to full screen
     *
     * @param view the view being used
     * @param savedInstanceState bundle re-constructed from a previous saved state
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setDialogLayoutToFullScreen()
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Sets the dialog layout to fullscreen
     */
    private fun setDialogLayoutToFullScreen() {
        val window = dialog?.window ?: return
        val wlp = window.attributes
        window.requestFeature(Window.FEATURE_NO_TITLE)
        wlp.gravity = Gravity.BOTTOM
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT
        wlp.height = WindowManager.LayoutParams.MATCH_PARENT
        window.attributes = wlp
    }

    /**
     * Builds custom dialog container
     *
     * @param savedInstanceState the previously saved state
     * @return the dialog
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }
}
