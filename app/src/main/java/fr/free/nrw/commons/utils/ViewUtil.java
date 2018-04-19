package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.Display;
import android.view.View;
import android.widget.Toast;

public class ViewUtil {

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

}
