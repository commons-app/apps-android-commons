package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;

import org.apache.commons.lang3.StringUtils;

import fr.free.nrw.commons.R;
import timber.log.Timber;

public class DialogUtil {

    /**
     * Shows a dialog safely.
     * @param activity the activity
     * @param dialog the dialog to be shown
     */
    private static void showSafely(Activity activity, Dialog dialog) {
        if (activity == null || dialog == null) {
            Timber.d("Show called with null activity / dialog. Ignoring.");
            return;
        }

        boolean isActivityDestroyed = activity.isDestroyed();
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

    public static void showAlertDialog(Activity activity,
                                       String title,
                                       String message,
                                       final Runnable onPositiveBtnClick,
                                       final Runnable onNegativeBtnClick) {
        showAlertDialog(activity,
                title,
                message,
                activity.getString(R.string.yes),
                activity.getString(R.string.no),
                onPositiveBtnClick,
                onNegativeBtnClick);
    }

    public static void showAlertDialog(Activity activity,
                                       String title,
                                       String message,
                                       String positiveButtonText,
                                       String negativeButtonText,
                                       final Runnable onPositiveBtnClick,
                                       final Runnable onNegativeBtnClick) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);

        if (!StringUtils.isBlank(positiveButtonText)) {
            builder.setPositiveButton(positiveButtonText, (dialogInterface, i) -> {
                dialogInterface.dismiss();
                if (onPositiveBtnClick != null) {
                    onPositiveBtnClick.run();
                }
            });
        }

        if (!StringUtils.isBlank(negativeButtonText)) {
            builder.setNegativeButton(negativeButtonText, (DialogInterface dialogInterface, int i) -> {
                dialogInterface.dismiss();
                if (onNegativeBtnClick != null) {
                    onNegativeBtnClick.run();
                }
            });
        }

        AlertDialog dialog = builder.create();
        showSafely(activity, dialog);
    }

    /*
    Shows alert dialog with custom view
    */
    public static void showAlertDialog(Activity activity,
                                       String title,
                                       String message,
                                       final Runnable onPositiveBtnClick,
                                       final Runnable onNegativeBtnClick,
                                       View customView,
                                       boolean cancelable) {
        showAlertDialog(activity,
                title,
                message,
                activity.getString(R.string.yes),
                activity.getString(R.string.no),
                onPositiveBtnClick,
                onNegativeBtnClick,
                customView,
                cancelable);
    }

    /*
    Shows alert dialog with custom view
     */
    private static void showAlertDialog(Activity activity,
                                        String title,
                                        String message,
                                        String positiveButtonText,
                                        String negativeButtonText,
                                        final Runnable onPositiveBtnClick,
                                        final Runnable onNegativeBtnClick,
                                        View customView,
                                        boolean cancelable) {
        // If the custom view already has a parent, there is already a dialog showing with the view
        // This happens for on resume - return to avoid creating a second dialog - the first one
        // will still show
        if (customView != null && customView.getParent() != null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setView(customView);
        builder.setCancelable(cancelable);

        builder.setPositiveButton(positiveButtonText, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            if (onPositiveBtnClick != null) {
                onPositiveBtnClick.run();
            }
        });

        builder.setNegativeButton(negativeButtonText, (DialogInterface dialogInterface, int i) -> {
            dialogInterface.dismiss();
            if (onNegativeBtnClick != null) {
                onNegativeBtnClick.run();
            }
        });

        AlertDialog dialog = builder.create();
        showSafely(activity, dialog);
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
    public static void showAlertDialog(Activity activity, String title, String message, String positiveButtonText, final Runnable positiveButtonClick, boolean cancellable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(cancellable);

        builder.setPositiveButton(positiveButtonText, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            if (positiveButtonClick != null) {
                positiveButtonClick.run();
            }
        });

        AlertDialog dialog = builder.create();
        showSafely(activity, dialog);
    }

}
