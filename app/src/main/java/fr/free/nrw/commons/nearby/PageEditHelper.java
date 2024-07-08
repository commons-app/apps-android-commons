package fr.free.nrw.commons.nearby;


import static fr.free.nrw.commons.notification.NotificationHelper.NOTIFICATION_DELETE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.actions.PageEditClient;
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException;
import fr.free.nrw.commons.notification.NotificationHelper;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Observable;
import io.reactivex.Single;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Singleton class for making edits to wiki pages asynchronously using RxJava.
 *
 * @property notificationHelper A helper class for displaying notifications.
 * @property pageEditClient      A client for making page edit requests.
 * @property viewUtil            A utility class for common view operations.
 * @property username            The username used for page edits.
 * @constructor Initializes the PageEditHelper with required dependencies.
 */
@Singleton
public class PageEditHelper {

    private final NotificationHelper notificationHelper;
    private final PageEditClient pageEditClient;
    private final ViewUtilWrapper viewUtil;
    private final String username;
    private AlertDialog d;
    private DialogInterface.OnMultiChoiceClickListener listener;

    @Inject
    public PageEditHelper(NotificationHelper notificationHelper,
        @Named("wikidata-page-edit") PageEditClient pageEditClient,
        ViewUtilWrapper viewUtil,
        @Named("username") String username) {
        this.notificationHelper = notificationHelper;
        this.pageEditClient = pageEditClient;
        this.viewUtil = viewUtil;
        this.username = username;
    }

    /**
     * Public interface to make a page edit request asynchronously.
     *
     * @param context     The context for displaying messages.
     * @param title       The title of the page to edit.
     * @param preText     The existing content of the page.
     * @param description The description of the issue to be fixed.
     * @param details     Additional details about the issue.
     * @param lat         The latitude of the location related to the page.
     * @param lng         The longitude of the location related to the page.
     * @return A Single emitting true if the edit was successful, false otherwise.
     */
    public Single<Boolean> makePageEdit(Context context, String title, String preText,
        String description,
        String details, Double lat, Double lng) {
        viewUtil.showShortToast(context, "Trying to edit " + title);

        return editPage(title, preText, description, details, lat, lng)
            .flatMapSingle(result -> Single.just(showNotification(context, title, result)))
            .firstOrError()
            .onErrorResumeNext(throwable -> {
                if (throwable instanceof InvalidLoginTokenException) {
                    return Single.error(throwable);
                }
                return Single.error(throwable);
            });
    }

    /**
     * Creates the text content for the page edit based on provided parameters.
     *
     * @param title       The title of the page to edit.
     * @param preText     The existing content of the page.
     * @param description The description of the issue to be fixed.
     * @param details     Additional details about the issue.
     * @param lat         The latitude of the location related to the page.
     * @param lng         The longitude of the location related to the page.
     * @return An Observable emitting true if the edit was successful, false otherwise.
     */
    private Observable<Boolean> editPage(String title, String preText, String description,
        String details, Double lat, Double lng) {
        Timber.d("thread is edit %s", Thread.currentThread().getName());
        String summary = "Please fix this item";
        String text = "";
        String marker = "Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!";
        int markerIndex = preText.indexOf(marker);
        if (preText == "" || markerIndex == -1) {
            text = "==Please fix this item==\n"
                + "Someone using the [[Commons:Mobile_app|Commons Android app]] went to this item's geographical location ("
                + lat + "," + lng
                + ") and noted the following problem(s):\n"
                + "* <i><nowiki>" + description + "</nowiki></i>\n"
                + "\n"
                + "Details: <i><nowiki>" + details + "</nowiki></i>\n"
                + "\n"
                + "Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!\n"
                + "\n"
                + "~~~~";
        } else {
            text = preText.substring(0, markerIndex);
            text = text + "* <i><nowiki>" + description + "</nowiki></i>\n"
                + "\n"
                + "Details: <i><nowiki>" + details + "</nowiki></i>\n"
                + "\n"
                + "Please anyone fix the item accordingly, then reply to mark this section as fixed. Thanks a lot for your cooperation!\n"
                + "\n"
                + "~~~~";
        }

        return pageEditClient.postCreate(title, text, summary);
    }

    /**
     * Displays a notification based on the result of the page edit.
     *
     * @param context The context for displaying the notification.
     * @param title   The title of the page edited.
     * @param result  The result of the edit operation.
     * @return true if the edit was successful, false otherwise.
     */
    private boolean showNotification(Context context, String title, boolean result) {
        String message;

        if (result) {
            message = title + " Edited Successfully";
        } else {
            message = context.getString(R.string.delete_helper_show_deletion_message_else);
        }

        String url = BuildConfig.WIKIDATA_URL + "/wiki/" + title;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        notificationHelper.showNotification(context, title, message, NOTIFICATION_DELETE,
            browserIntent);
        return result;
    }

    /**
     * returns the instance of shown AlertDialog, used for taking reference during unit test
     */
    public AlertDialog getDialog() {
        return d;
    }

    /**
     * returns the instance of shown DialogInterface.OnMultiChoiceClickListener, used for taking
     * reference during unit test
     */
    public DialogInterface.OnMultiChoiceClickListener getListener() {
        return listener;
    }
}
