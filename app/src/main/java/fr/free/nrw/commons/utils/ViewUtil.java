package fr.free.nrw.commons.utils;

import android.support.design.widget.Snackbar;
import android.view.View;

public class ViewUtil {

    public static void showSnackbar(View view, int messageResourceId) {
        Snackbar.make(view, messageResourceId, Snackbar.LENGTH_SHORT).show();
    }

}
