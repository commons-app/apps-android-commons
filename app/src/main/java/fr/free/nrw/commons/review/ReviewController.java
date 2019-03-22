package fr.free.nrw.commons.review;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.delete.DeleteTask;
import fr.free.nrw.commons.di.ApplicationlessInjection;
import fr.free.nrw.commons.media.model.MwQueryPage;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@Singleton
public class ReviewController {
    private String fileName;
    @Nullable
    public MwQueryPage.Revision firstRevision; // TODO: maybe we can expand this class to include fileName
    protected static ArrayList<String> categories;
    public static final int NOTIFICATION_SEND_THANK = 0x102;
    public static final int NOTIFICATION_CHECK_CATEGORY = 0x101;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Media media;

    @Inject
    MediaWikiApi mwApi;
    @Inject
    SessionManager sessionManager;

    public void onImageRefreshed(String fileName) {
        this.fileName = fileName;
        media = new Media("File:" + fileName);
        ReviewController.categories = new ArrayList<>();
    }

    public void onCategoriesRefreshed(ArrayList<String> categories) {
        ReviewController.categories = categories;
    }

    public void reportSpam(@NonNull Activity activity) {
        DeleteTask.askReasonAndExecute(new Media("File:" + fileName),
                activity,
                activity.getString(R.string.review_spam_report_question),
                activity.getString(R.string.review_spam_report_problem));
    }

    public void reportPossibleCopyRightViolation(@NonNull Activity activity) {
        DeleteTask.askReasonAndExecute(new Media("File:" + fileName),
                activity,
                activity.getResources().getString(R.string.review_c_violation_report_question),
                activity.getResources().getString(R.string.review_c_violation_report_problem));
    }

    @SuppressLint("CheckResult")
    public void reportWrongCategory(@NonNull Activity activity) {
        Context context = activity.getApplicationContext();
        ApplicationlessInjection
                .getInstance(context)
                .getCommonsApplicationComponent()
                .inject(this);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
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
                if (editToken.equals("+\\")) {
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
                    String message = "";
                    String title = "";

                    if (result) {
                        title = context.getString(R.string.check_category_success_title);
                        message = context.getString(R.string.check_category_success_message, media.getDisplayTitle());
                    } else {
                        title = context.getString(R.string.check_category_failure_title);
                        message = context.getString(R.string.check_category_failure_message, media.getDisplayTitle());
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

    @SuppressLint("CheckResult")
    public void sendThanks(@NonNull Activity activity) {
        Context context = activity.getApplicationContext();
        ApplicationlessInjection
                .getInstance(context)
                .getCommonsApplicationComponent()
                .inject(this);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(context);
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
                if (editToken.equals("+\\")) {
                    return false;
                }
                publishProgress(context, 1);
                assert firstRevision != null;
                mwApi.thank(editToken, firstRevision.getRevid());
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
}
