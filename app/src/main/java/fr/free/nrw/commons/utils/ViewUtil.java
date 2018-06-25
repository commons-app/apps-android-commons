package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

public class ViewUtil {

    public static final String SHOWCASE_VIEW_ID_1 = "SHOWCASE_VIEW_ID_1";
    public static final String SHOWCASE_VIEW_ID_2 = "SHOWCASE_VIEW_ID_2";
    public static final String SHOWCASE_VIEW_ID_3 = "SHOWCASE_VIEW_ID_3";

    public static void showSnackbar(View view, int messageResourceId) {
        Snackbar.make(view, messageResourceId, Snackbar.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context context, String text) {
        Toast.makeText(context, text,
                Toast.LENGTH_LONG).show();
    }

    public static boolean isPortrait(Context context) {
        Display orientation = ((Activity)context).getWindowManager().getDefaultDisplay();
        if(orientation.getWidth() < orientation.getHeight()){
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

}
