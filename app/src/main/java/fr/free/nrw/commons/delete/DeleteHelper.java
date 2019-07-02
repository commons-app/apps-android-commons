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
        viewUtil.showShortToast(context, context.getResources().getString(R.string.delete_helper_make_deletion_toast_1)
                + media.getDisplayTitle() + context.getResources().getString(R.string.delete_helper_make_deletion_toast_2));
        return Single.fromCallable(() -> delete(context, media, reason))
                .flatMap(result -> Single.fromCallable(() ->
                        showDeletionNotification(context, media, result)));
    }

    /**
     * Makes several API calls to nominate the file for deletion
     * @param media
     * @param reason
     * @return
     */
    private boolean delete(Context context, Media media, String reason) {
        String editToken;
        String authCookie;
        String summary = context.getResources().getString(R.string.delete_helper_delete_summary_1)
                + media.getFilename() + context.getResources().getString(R.string.delete_helper_delete_summary_2);

        authCookie = sessionManager.getAuthCookie();
        mwApi.setAuthCookie(authCookie);

        Calendar calendar = Calendar.getInstance();
        String fileDeleteString = context.getResources().getString(R.string.delete_helper_delete_file_delete_string_1) + reason +
                context.getResources().getString(R.string.delete_helper_delete_file_delete_string_2) + media.getFilename() +
                context.getResources().getString(R.string.delete_helper_delete_file_delete_string_3) + calendar.get(Calendar.DAY_OF_MONTH) +
                context.getResources().getString(R.string.delete_helper_delete_file_delete_string_4) + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) +
                context.getResources().getString(R.string.delete_helper_delete_file_delete_string_5) + calendar.get(Calendar.YEAR) +
                "}}";

        String subpageString = "=== [[:" + media.getFilename() + "]] ===\n" +
                reason +
                " ~~~~";

        String logPageString = "\n" + context.getResources().getString(R.string.delete_helper_delete_log_page_string) + media.getFilename() +
                "}}\n";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        String date = sdf.format(calendar.getTime());

        String userPageString = "\n" + context.getResources().getString(R.string.delete_helper_delete_user_page_string) + media.getFilename() +
                "}} ~~~~";

        try {
            editToken = mwApi.getEditToken();

            if(editToken == null) {
                return false;
            }

            mwApi.prependEdit(editToken, fileDeleteString + "\n",
                    media.getFilename(), summary);
            mwApi.edit(editToken, subpageString + "\n",
                    context.getResources().getString(R.string.delete_helper_delete_try_commons) + media.getFilename(), summary);
            mwApi.appendEdit(editToken, logPageString + "\n",
                    context.getResources().getString(R.string.delete_helper_delete_try_commons) + date, summary);
            mwApi.appendEdit(editToken, userPageString + "\n",
                    context.getResources().getString(R.string.delete_helper_delete_try_talk) + media.getCreator(), summary);
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
        return true;
    }

    private boolean showDeletionNotification(Context context, Media media, boolean result) {
        String message;
        String title = context.getResources().getString(R.string.delete_helper_show_deletion_notification_title);

        if (result) {
            title += context.getResources().getString(R.string.delete_helper_show_deletion_notification_title_if);
            message = context.getResources().getString(R.string.delete_helper_show_deletion_notification_message_if_1)
                    + media.getDisplayTitle() + context.getResources().getString(R.string.delete_helper_show_deletion_notification_message_if_2);
        } else {
            title += context.getResources().getString(R.string.delete_helper_show_deletion_notification_title_else);
            message = context.getResources().getString(R.string.delete_helper_show_deletion_notification_message_else);
        }

        String urlForDelete = BuildConfig.COMMONS_URL + context.getResources().getString(R.string.delete_helper_show_deletion_notification_url_for_delete) + media.getFilename();
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
            reasonList[0] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_spam_selfie);
            reasonList[1] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_spam_blurry);
            reasonList[2] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_spam_nonsense);
            reasonList[3] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_spam_other);
        } else if (problem == ReviewController.DeleteReason.COPYRIGHT_VIOLATION) {
            reasonList[0] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_copyright_press_photo);
            reasonList[1] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_copyright_internet_photo);
            reasonList[2] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_copyright_logo);
            reasonList[3] = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_reason_copyright_other);
        }

        alert.setMultiChoiceItems(reasonList, checkedItems, (dialogInterface, position, isChecked) -> {
            if (isChecked) {
                mUserReason.add(position);
            } else {
                mUserReason.remove((Integer.valueOf(position)));
            }
        });

        alert.setPositiveButton(context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_alert_set_positive_button), (dialogInterface, i) -> {

            String reason = context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_alert_set_positive_button_reason);
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
        alert.setNegativeButton(context.getResources().getString(R.string.delete_helper_ask_reason_and_execute_alert_set_negative_button), (dialog, which) -> reviewCallback.onFailure());
        AlertDialog d = alert.create();
        d.show();
    }
}
