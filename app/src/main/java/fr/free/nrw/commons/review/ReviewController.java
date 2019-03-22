package fr.free.nrw.commons.review;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.viewpager.widget.ViewPager;
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

    private ReviewPagerAdapter reviewPagerAdapter;
    private ViewPager viewPager;
    private ReviewActivity reviewActivity;

    ReviewController(Context context) {
        reviewActivity = (ReviewActivity) context;
        reviewPagerAdapter = reviewActivity.reviewPagerAdapter;
        viewPager = ((ReviewActivity) context).reviewPager;
    }

    public void onImageRefreshed(String fileName) {
        this.fileName = fileName;
        media = new Media("File:" + fileName);
        ReviewController.categories = new ArrayList<>();
    }

    public void onCategoriesRefreshed(ArrayList<String> categories) {
        ReviewController.categories = categories;
    }

    public void swipeToNext() {
        int nextPos = viewPager.getCurrentItem() + 1;
        if (nextPos <= 3) {
            viewPager.setCurrentItem(nextPos);
        } else {
            reviewActivity.runRandomizer();
        }
    }

    public void reportSpam() {
        DeleteTask.askReasonAndExecute(new Media("File:" + fileName),
                reviewActivity,
                reviewActivity.getResources().getString(R.string.review_spam_report_question),
                reviewActivity.getResources().getString(R.string.review_spam_report_problem));
    }

    public void reportPossibleCopyRightViolation() {
        DeleteTask.askReasonAndExecute(new Media("File:" + fileName),
                reviewActivity,
                reviewActivity.getResources().getString(R.string.review_c_violation_report_question),
                reviewActivity.getResources().getString(R.string.review_c_violation_report_problem));
    }

    @SuppressLint("CheckResult")
    public void reportWrongCategory() {
        ApplicationlessInjection
                .getInstance(reviewActivity.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);

        notificationManager = (NotificationManager) reviewActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(reviewActivity);
        Toast toast = new Toast(reviewActivity);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast = Toast.makeText(reviewActivity, reviewActivity.getString(R.string.check_category_toast, media.getDisplayTitle()), Toast.LENGTH_SHORT);
        toast.show();

        Observable.fromCallable(() -> {
            publishProgress(0);

            String editToken;
            String authCookie;
            String summary = reviewActivity.getString(R.string.check_category_edit_summary);

            authCookie = sessionManager.getAuthCookie();
            mwApi.setAuthCookie(authCookie);

            try {
                editToken = mwApi.getEditToken();
                if (editToken.equals("+\\")) {
                    return false;
                }
                publishProgress(1);

                mwApi.appendEdit(editToken, "\n{{subst:chc}}\n", media.getFilename(), summary);
                publishProgress(2);
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
                        title = reviewActivity.getString(R.string.check_category_success_title);
                        message = reviewActivity.getString(R.string.check_category_success_message, media.getDisplayTitle());
                    } else {
                        title = reviewActivity.getString(R.string.check_category_failure_title);
                        message = reviewActivity.getString(R.string.check_category_failure_message, media.getDisplayTitle());
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
        swipeToNext();
    }

    private void publishProgress(int i) {
        int[] messages = new int[]{R.string.getting_edit_token, R.string.check_category_adding_template};
        String message = "";
        if (0 < i && i < messages.length) {
            message = reviewActivity.getString(messages[i]);
        }

        notificationBuilder.setContentTitle(reviewActivity.getString(R.string.check_category_notification_title, media.getDisplayTitle()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setProgress(messages.length, i, false)
                .setOngoing(true);
        notificationManager.notify(NOTIFICATION_CHECK_CATEGORY, notificationBuilder.build());
    }

    @SuppressLint("CheckResult")
    public void sendThanks() {
        ApplicationlessInjection
                .getInstance(reviewActivity.getApplicationContext())
                .getCommonsApplicationComponent()
                .inject(this);
        notificationManager = (NotificationManager) reviewActivity.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationBuilder = new NotificationCompat.Builder(reviewActivity);
        Toast toast = new Toast(reviewActivity);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast = Toast.makeText(reviewActivity, reviewActivity.getString(R.string.send_thank_toast, media.getDisplayTitle()), Toast.LENGTH_SHORT);
        toast.show();

        Observable.fromCallable(() -> {
            publishProgress(0);

            String editToken;
            String authCookie;
            authCookie = sessionManager.getAuthCookie();
            mwApi.setAuthCookie(authCookie);

            try {
                editToken = mwApi.getEditToken();
                if (editToken.equals("+\\")) {
                    return false;
                }
                publishProgress(1);
                assert firstRevision != null;
                mwApi.thank(editToken, firstRevision.getRevid());
                publishProgress(2);
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
                        title = reviewActivity.getString(R.string.send_thank_success_title);
                        message = reviewActivity.getString(R.string.send_thank_success_message, media.getDisplayTitle());
                    } else {
                        title = reviewActivity.getString(R.string.send_thank_failure_title);
                        message = reviewActivity.getString(R.string.send_thank_failure_message, media.getDisplayTitle());
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
        swipeToNext();
    }
}
