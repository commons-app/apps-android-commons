package fr.free.nrw.commons.ui.widget;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * a formatted dialog fragment
 * This class is used by NearbyInfoDialog
 */
public abstract class OverlayDialog extends DialogFragment {

    /**
     * creates a DialogFragment with the correct style and theme
     * @param savedInstanceState bundle re-constructed from a previous saved state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Holo_Light);
    }

    /**
     * When the view is created, sets the dialog layout to full screen
     * 
     * @param view the view being used
     * @param savedInstanceState bundle re-constructed from a previous saved state
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setDialogLayoutToFullScreen();
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * sets the dialog layout to fullscreen
     */
    private void setDialogLayoutToFullScreen() {
        Window window = getDialog().getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        window.requestFeature(Window.FEATURE_NO_TITLE);
        wlp.gravity = Gravity.BOTTOM;
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
        wlp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(wlp);
    }

    /**
     * builds custom dialog container
     * 
     * @param savedInstanceState the previously saved state
     * @return the dialog
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }
}
