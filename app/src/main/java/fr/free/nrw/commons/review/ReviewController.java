package fr.free.nrw.commons.review;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.wikipedia.dataclient.mwapi.MwQueryPage;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@Singleton
public class ReviewController {
    private static final int NOTIFICATION_SEND_THANK = 0x102;
    private static final int NOTIFICATION_CHECK_CATEGORY = 0x101;
    private final DeleteHelper deleteHelper;
    @Nullable
    MwQueryPage.Revision firstRevision; // TODO: maybe we can expand this class to include fileName
    @Inject
    MediaWikiApi mwApi;
    @Inject
    SessionManager sessionManager;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Media media;

    ReviewController(DeleteHelper deleteHelper, Context context) {
        this.deleteHelper = deleteHelper;
        CommonsApplication.createNotificationChannel(context.getApplicationContext());
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context, CommonsApplication.NOTIFICATION_CHANNEL_ID_ALL);
    }

    void onImageRefreshed(Media media) {
        this.media = media;
    }

    public Media getMedia() {
        return media;
    }

    public enum DeleteReason {
        SPAM,
        COPYRIGHT_VIOLATION
    }

    void reportSpam(@NonNull Activity activity, ReviewCallback reviewCallback) {
        Timber.d("Report spam for %s", media.getFilename());
        deleteHelper.askReasonAndExecute(media,
                activity,
                activity.getResources().getString(R.string.review_spam_report_question),
                DeleteReason.SPAM,
                reviewCallback);
    }

    void reportPossibleCopyRightViolation(@NonNull Activity activity, ReviewCallback reviewCallback) {
        Timber.d("Report spam for %s", media.getFilename());
        deleteHelper.askReasonAndExecute(media,
                activity,
                activity.getResources().getString(R.string.review_c_violation_report_question),
                DeleteReason.COPYRIGHT_VIOLATION,
                reviewCallback);
    }

    @SuppressLint("CheckResult")
    void reportWrongCategory(@NonNull Activity activity, ReviewCallback reviewCallback) {
        Context context = activity.getApplicationContext();
        ApplicationlessInjection
                .getInstance(context)
                .getCommonsApplicationComponent()
                .inject(this);

        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast = Toast.makeText(context, context.getString(R.string.check_category_toast, media.getDisplayTitle()), Toast.LENGTH_SHORT);
        toast.show();

        Observable.fromCallable(() -> {
            publishProgress(context, 0);

            String editToken;
            String authCookie;
            String summary = context.getString(R.string.check_category_edit_summary);

            authCookie = sessionManager.getAuthCookie();
            mwApi.setAuthCookie(authCookie);

            try {
                editToken = mwApi.getEditToken();

                if (editToken == null) {
                    return false;
                }
                publishProgress(context, 1);

                mwApi.appendEdit(editToken, "\n{{subst:chc}}\n", media.getFilename(), summary);
                publishProgress(context, 2);
            } catch (Exception e) {
                Timber.d(e);
                return false;
            }
            return true;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    String message;
                    String title;

                    if (result) {
                        title = context.getString(R.string.check_category_success_title);
                        message = context.getString(R.string.check_category_success_message, media.getDisplayTitle());
                        reviewCallback.onSuccess();
                    } else {
                        title = context.getString(R.string.check_category_failure_title);
                        message = context.getString(R.string.check_category_failure_message, media.getDisplayTitle());
                        reviewCallback.onFailure();
                    }

                    notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setContentTitle(title)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(message))
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setProgress(0, 0, false)
                            .setOngoing(false)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);
                    notificationManager.notify(NOTIFICATION_CHECK_CATEGORY, notificationBuilder.build());

                }, Timber::e);
    }

    private void publishProgress(@NonNull Context context, int i) {
        int[] messages = new int[]{R.string.getting_edit_token, R.string.check_category_adding_template};
        String message = "";
        if (0 < i && i < messages.length) {
            message = context.getString(messages[i]);
        }

        notificationBuilder.setContentTitle(context.getString(R.string.check_category_notification_title, media.getDisplayTitle()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(messages.length, i, false)
                .setOngoing(true);
        notificationManager.notify(NOTIFICATION_CHECK_CATEGORY, notificationBuilder.build());
    }

    @SuppressLint({"CheckResult", "StringFormatInvalid"})
    void sendThanks(@NonNull Activity activity) {
        Context context = activity.getApplicationContext();
        ApplicationlessInjection
                .getInstance(context)
                .getCommonsApplicationComponent()
                .inject(this);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast = Toast.makeText(context, context.getString(R.string.send_thank_toast, media.getDisplayTitle()), Toast.LENGTH_SHORT);
        toast.show();

        Observable.fromCallable(() -> {
            publishProgress(context, 0);

            String editToken;
            String authCookie;
            authCookie = sessionManager.getAuthCookie();
            mwApi.setAuthCookie(authCookie);

            try {
                editToken = mwApi.getEditToken();
                if (editToken == null) {
                    return false;
                }
                publishProgress(context, 1);
                assert firstRevision != null;
                mwApi.thank(editToken, firstRevision.getRevisionId());
                publishProgress(context, 2);
            } catch (Exception e) {
                Timber.d(e);
                return false;
            }
            return true;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    String message = "";
                    String title = "";
                    if (result) {
                        title = context.getString(R.string.send_thank_success_title);
                        message = context.getString(R.string.send_thank_success_message, media.getDisplayTitle());
                    } else {
                        title = context.getString(R.string.send_thank_failure_title);
                        message = context.getString(R.string.send_thank_failure_message, media.getDisplayTitle());
                    }

                    notificationBuilder.setDefaults(NotificationCompat.DEFAULT_ALL)
                            .setContentTitle(title)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .bigText(message))
                            .setSmallIcon(R.drawable.ic_launcher)
                            .setProgress(0, 0, false)
                            .setOngoing(false)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);
                    notificationManager.notify(NOTIFICATION_SEND_THANK, notificationBuilder.build());

                }, Timber::e);
    }

    public interface ReviewCallback {
        void onSuccess();

        void onFailure();
    }
}
