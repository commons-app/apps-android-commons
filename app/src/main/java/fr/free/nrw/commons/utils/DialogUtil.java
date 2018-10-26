package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import fr.free.nrw.commons.R;
import timber.log.Timber;

public class DialogUtil {

    /**
     * Dismisses a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be dismissed
     */
    public static void dismissSafely(@Nullable Activity activity, @Nullable DialogFragment dialog) {
        boolean isActivityDestroyed = false;

        if (activity == null || dialog == null) {
            Timber.d("dismiss called with null activity / dialog. Ignoring.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isActivityDestroyed = activity.isDestroyed();
        }
        if (activity.isFinishing() || isActivityDestroyed) {
            return;
        }
        try {
            dialog.dismiss();

        } catch (IllegalStateException e) {
            Timber.e(e, "Could not dismiss dialog.");
        }
    }

    /**
     * Shows a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be shown
     */
    public static void showSafely(Activity activity, Dialog dialog) {
        if (activity == null || dialog == null) {
            Timber.d("Show called with null activity / dialog. Ignoring.");
            return;
        }

        boolean isActivityDestroyed = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isActivityDestroyed = activity.isDestroyed();
        }
        if (activity.isFinishing() || isActivityDestroyed) {
            Timber.e("Activity is not running. Could not show dialog. ");
            return;
        }
        try {
            dialog.show();
        } catch (IllegalStateException e) {
            Timber.e(e, "Could not show dialog.");
        }
    }

    /**
     * Shows a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be shown
     */
    public static void showSafely(FragmentActivity activity, DialogFragment dialog) {
        boolean isActivityDestroyed = false;

        if (activity == null || dialog == null) {
            Timber.d("show called with null activity / dialog. Ignoring.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isActivityDestroyed = activity.isDestroyed();
        }
        if (activity.isFinishing() || isActivityDestroyed) {
            return;
        }

        try {
            if (dialog.getDialog() == null || !dialog.getDialog().isShowing()) {
                dialog.show(activity.getSupportFragmentManager(), dialog.getClass().getSimpleName());
            }
        } catch (IllegalStateException e) {
            Timber.e(e, "Could not show dialog.");
        }
    }

    public static AlertDialog getAlertDialogWithPositiveAndNegativeCallbacks(
            Context context, String title, String message, int iconResourceId, Callback callback) {

        AlertDialog alertDialog = new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                    callback.onPositiveButtonClicked();
                    dialog.dismiss();
                })
                .setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> {
                    callback.onNegativeButtonClicked();
                    dialog.dismiss();
                })
                .setIcon(iconResourceId).create();

        return alertDialog;

    }

    public  interface Callback {

        void onPositiveButtonClicked();

        void onNegativeButtonClicked();
    }
}
