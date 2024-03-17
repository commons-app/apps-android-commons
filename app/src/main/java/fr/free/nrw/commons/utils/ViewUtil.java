package fr.free.nrw.commons.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Display;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;

import fr.free.nrw.commons.R;
import timber.log.Timber;

public class ViewUtil {
    /**
     * Utility function to show short snack bar
     * @param view
     * @param messageResourceId
     */
    public static void showShortSnackbar(View view, int messageResourceId) {
        if (view.getContext() == null) {
            return;
        }

        ExecutorUtils.uiExecutor().execute(() -> {
            try {
                Snackbar.make(view, messageResourceId, Snackbar.LENGTH_SHORT).show();
            }catch (IllegalStateException e){
                Timber.e(e.getMessage());
            }
        });
    }
    public static void showLongSnackbar(View view, String text) {
        if(view.getContext() == null) {
            return;
        }

        ExecutorUtils.uiExecutor().execute(()-> {
            try {
                Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_SHORT);

                View snack_view = snackbar.getView();
                TextView snack_text = snack_view.findViewById(R.id.snackbar_text);

                snack_view.setBackgroundColor(Color.LTGRAY);
                snack_text.setTextColor(ContextCompat.getColor(view.getContext(), R.color.primaryColor));
                snackbar.setActionTextColor(Color.RED);

                snackbar.setAction("Dismiss", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Handle the action click
                        snackbar.dismiss();
                    }
                });

                snackbar.show();

            }catch (IllegalStateException e) {
                Timber.e(e.getMessage());
            }
        });
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
