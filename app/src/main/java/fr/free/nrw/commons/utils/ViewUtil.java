package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class ViewUtil {

    public static final String SHOWCASE_VIEW_ID_1 = "SHOWCASE_VIEW_ID_1";
    public static final String SHOWCASE_VIEW_ID_2 = "SHOWCASE_VIEW_ID_2";
    public static final String SHOWCASE_VIEW_ID_3 = "SHOWCASE_VIEW_ID_3";

    /**
     * Utility function to show short snack bar
     * @param view
     * @param messageResourceId
     */
    public static void showShortSnackbar(View view, int messageResourceId) {
        if (view.getContext() == null) {
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Snackbar.make(view, messageResourceId, Snackbar.LENGTH_SHORT).show());
    }

    public static void showLongToast(Context context, String text) {
        if (context == null) {
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
    }

    public static void showLongToast(Context context, @StringRes int stringResourceId) {
        if (context == null) {
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Toast.makeText(context, context.getString(stringResourceId), Toast.LENGTH_LONG).show());
    }

    public static void showShortToast(Context context, String text) {
        if (context == null) {
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Toast.makeText(context, text, Toast.LENGTH_SHORT).show());
    }

    public static void showShortToast(Context context, @StringRes int stringResourceId) {
        if (context == null) {
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Toast.makeText(context, context.getString(stringResourceId), Toast.LENGTH_SHORT).show());
    }

    public static boolean isPortrait(Context context) {
        Display orientation = ((Activity)context).getWindowManager().getDefaultDisplay();
        if (orientation.getWidth() < orientation.getHeight()){
            return true;
        } else {
            return false;
        }
    }

    public static void hideKeyboard(View view){
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            view.clearFocus();
            if (manager != null) {
                manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /**
     * A snack bar which has an action button which on click dismisses the snackbar and invokes the
     * listener passed
     */
    public static void showDismissibleSnackBar(View view,
                                               int messageResourceId,
                                               int actionButtonResourceId,
                                               View.OnClickListener onClickListener) {
        if (view.getContext() == null) {
            return;
        }
        ExecutorUtils.uiExecutor().execute(() -> {
            Snackbar snackbar = Snackbar.make(view, view.getContext().getString(messageResourceId),
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction(view.getContext().getString(actionButtonResourceId), v -> {
                snackbar.dismiss();
                onClickListener.onClick(v);
            });
            snackbar.show();
        });
    }
}
