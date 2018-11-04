package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.support.design.widget.Snackbar;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import timber.log.Timber;

public class ViewUtils {

    public static final String SHOWCASE_VIEW_ID_1 = "SHOWCASE_VIEW_ID_1";
    public static final String SHOWCASE_VIEW_ID_2 = "SHOWCASE_VIEW_ID_2";
    public static final String SHOWCASE_VIEW_ID_3 = "SHOWCASE_VIEW_ID_3";

    /**
     * Show a short snackbar
     * @param view View to show a snackbar in
     * @param messageResourceId ResourceId of text to show in snackbar
     */
    public static void showSnackbar(View view, int messageResourceId) {
        if (view.getContext() == null) {
            Timber.d("Tried to show a snackbar, but was given a null context");
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Snackbar.make(view, messageResourceId, Snackbar.LENGTH_SHORT).show());
    }

    /**
     * Show a long toast
     * @param context Context to show a long toast in
     * @param messageResourceId ResourceId of text to show in toast
     */
    public static void showLongToast(Context context, int messageResourceId) {
        if (context == null) {
            Timber.d("Tried to show a long toast, but was given a null context");
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Toast.makeText(context, messageResourceId, Toast.LENGTH_LONG).show());
    }

    /**
     * Show a long toast
     * @param context Context to show a long toast in
     * @param text String of text to show in toast
     */
    public static void showLongToast(Context context, String text) {
        if (context == null) {
            Timber.d("Tried to show a long toast, but was given a null context");
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
    }

    /**
     * Checks whether screen is portrait
     * @param context App context
     * @return true if screen is portrait, false otherwise
     */
    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * Hides the keyboard from a view
     * @param view View to hide keyboard in
     */
    public static void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager manager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            view.clearFocus();
            if (manager != null) {
                manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

}
