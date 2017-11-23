package fr.free.nrw.commons.utils;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

public class ViewUtil {

    public static void showLongToast(final Context context, @StringRes final int stringResId) {
        ExecutorUtils.uiExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_LONG).show();
            }
        });
    }
}
