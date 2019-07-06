package fr.free.nrw.commons.delete;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.notification.NotificationHelper;
import fr.free.nrw.commons.review.ReviewController;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.notification.NotificationHelper.NOTIFICATION_DELETE;

/**
 * Refactored async task to Rx
 */
@Singleton
public class DeleteHelper {
    private final MediaWikiApi mwApi;
    private final SessionManager sessionManager;
    private final NotificationHelper notificationHelper;
    private final ViewUtilWrapper viewUtil;

    @Inject
    public DeleteHelper(MediaWikiApi mwApi,
                        SessionManager sessionManager,
                        NotificationHelper notificationHelper,
                        ViewUtilWrapper viewUtil) {
        this.mwApi = mwApi;
        this.sessionManager = sessionManager;
        this.notificationHelper = notificationHelper;
        this.viewUtil = viewUtil;
    }

    /**
     * Public interface to nominate a particular media file for deletion
     * @param context
     * @param media
     * @param reason
     * @return
     */
    public Single<Boolean> makeDeletion(Context context, Media media, String reason) {
        viewUtil.showShortToast(context, String.format(context.getString(R.string.delete_helper_make_deletion_toast), media.getDisplayTitle()));
        return Single.fromCallable(() -> delete(media, reason))
                .flatMap(result -> Single.fromCallable(() ->
                        showDeletionNotification(context, media, result)));
    }

    /**
     * Makes several API calls to nominate the file for deletion
     * @param media
     * @param reason
     * @return
     */
    private boolean delete(Media media, String reason) {
        String editToken;
        String authCookie;
        String summary = "Nominating " + media.getFilename() + " for deletion.";

        authCookie = sessionManager.getAuthCookie();
        mwApi.setAuthCookie(authCookie);

        Calendar calendar = Calendar.getInstance();
        String fileDeleteString = "{{delete|reason=" + reason +
                "|subpage=" + media.getFilename() +
                "|day=" + calendar.get(Calendar.DAY_OF_MONTH) +
                "|month=" + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) +
                "|year=" + calendar.get(Calendar.YEAR) +
                "}}";

        String subpageString = "=== [[:" + media.getFilename() + "]] ===\n" +
                reason +
                " ~~~~";

        String logPageString = "\n{{Commons:Deletion requests/" + media.getFilename() +
                "}}\n";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String date = sdf.format(calendar.getTime());

        String userPageString = "\n{{subst:idw|" + media.getFilename() +
                "}} ~~~~";

        try {
            editToken = mwApi.getEditToken();

            if(editToken == null) {
                return false;
            }

            mwApi.prependEdit(editToken, fileDeleteString + "\n",
                    media.getFilename(), summary);
            mwApi.edit(editToken, subpageString + "\n",
                    "Commons:Deletion_requests/" + media.getFilename(), summary);
            mwApi.appendEdit(editToken, logPageString + "\n",
                    "Commons:Deletion_requests/" + date, summary);
            mwApi.appendEdit(editToken, userPageString + "\n",
                    "User_Talk:" + media.getCreator(), summary);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
        return true;
    }

    private boolean showDeletionNotification(Context context, Media media, boolean result) {
        String message;
        String title = context.getString(R.string.delete_helper_show_deletion_title);

        if (result) {
            title += ": " + context.getString(R.string.delete_helper_show_deletion_title_success);
            message = String.format(context.getString(R.string.delete_helper_show_deletion_message_if), media.getDisplayTitle());
        } else {
            title += ": " + context.getString(R.string.delete_helper_show_deletion_title_failed);
            message = context.getString(R.string.delete_helper_show_deletion_message_else) ;
        }

        String urlForDelete = BuildConfig.COMMONS_URL + "/wiki/Commons:Deletion_requests/" + media.getFilename();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForDelete));
        notificationHelper.showNotification(context, title, message, NOTIFICATION_DELETE, browserIntent);
        return result;
    }

    /**
     * Invoked when a reason needs to be asked before nominating for deletion
     * @param media
     * @param context
     * @param question
     * @param problem
     */
    @SuppressLint("CheckResult")
    public void askReasonAndExecute(Media media,
                                    Context context,
                                    String question,
                                    ReviewController.DeleteReason problem,
                                    ReviewController.ReviewCallback reviewCallback) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setTitle(question);

        boolean[] checkedItems = {false, false, false, false};
        ArrayList<Integer> mUserReason = new ArrayList<>();

        String[] reasonList = {"Reason 1", "Reason 2", "Reason 3", "Reason 4"};


        if (problem == ReviewController.DeleteReason.SPAM) {
            reasonList[0] = context.getString(R.string.delete_helper_ask_spam_selfie);
            reasonList[1] = context.getString(R.string.delete_helper_ask_spam_blurry);
            reasonList[2] = context.getString(R.string.delete_helper_ask_spam_nonsense);
            reasonList[3] = context.getString(R.string.delete_helper_ask_spam_other);
        } else if (problem == ReviewController.DeleteReason.COPYRIGHT_VIOLATION) {
            reasonList[0] = context.getString(R.string.delete_helper_ask_reason_copyright_press_photo);
            reasonList[1] = context.getString(R.string.delete_helper_ask_reason_copyright_internet_photo);
            reasonList[2] = context.getString(R.string.delete_helper_ask_reason_copyright_logo);
            reasonList[3] = context.getString(R.string.delete_helper_ask_reason_copyright_other);
        }

        alert.setMultiChoiceItems(reasonList, checkedItems, (dialogInterface, position, isChecked) -> {
            if (isChecked) {
                mUserReason.add(position);
            } else {
                mUserReason.remove((Integer.valueOf(position)));
            }
        });

        alert.setPositiveButton(context.getString(R.string.ok), (dialogInterface, i) -> {

            String reason = context.getString(R.string.delete_helper_ask_alert_set_positive_button_reason) + " ";
            for (int j = 0; j < mUserReason.size(); j++) {
                reason = reason + reasonList[mUserReason.get(j)];
                if (j != mUserReason.size() - 1) {
                    reason = reason + ", ";
                }
            }

            makeDeletion(context, media, reason)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aBoolean -> {
                        if (aBoolean) {
                            reviewCallback.onSuccess();
                        } else {
                            reviewCallback.onFailure();
                        }
                    });

        });
        alert.setNegativeButton(context.getString(R.string.cancel), (dialog, which) -> reviewCallback.onFailure());
        AlertDialog d = alert.create();
        d.show();
    }
}
