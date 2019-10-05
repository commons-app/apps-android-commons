package fr.free.nrw.commons.quiz;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.DialogUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * fetches the number of images uploaded and number of images reverted.
 * Then it calculates the percentage of the images reverted
 * if the percentage of images reverted after last quiz exceeds 50% and number of images uploaded is
 * greater than 50, then quiz is popped up
 */
@Singleton
public class QuizChecker {

    private int revertCount ;
    private int totalUploadCount ;
    private boolean isRevertCountFetched;
    private boolean isUploadCountFetched;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final SessionManager sessionManager;
    private final OkHttpJsonApiClient okHttpJsonApiClient;
    private final JsonKvStore revertKvStore;

    private static final int UPLOAD_COUNT_THRESHOLD = 5;
    private static final String REVERT_PERCENTAGE_FOR_MESSAGE = "50%";
    private final String REVERT_SHARED_PREFERENCE = "revertCount";
    private final String UPLOAD_SHARED_PREFERENCE = "uploadCount";

    /**
     * constructor to set the parameters for quiz
     * @param sessionManager
     * @param okHttpJsonApiClient
     */
    @Inject
    public QuizChecker(SessionManager sessionManager,
                       OkHttpJsonApiClient okHttpJsonApiClient,
                       @Named("default_preferences") JsonKvStore revertKvStore) {
        this.sessionManager = sessionManager;
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.revertKvStore = revertKvStore;
    }

    public void initQuizCheck(Activity activity) {
        setUploadCount(activity);
        setRevertCount(activity);
    }

    public void cleanup() {
        compositeDisposable.clear();
    }

    /**
     * to fet the total number of images uploaded
     */
    private void setUploadCount(Activity activity) {
        compositeDisposable.add(okHttpJsonApiClient
                .getUploadCount(sessionManager.getUserName())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uploadCount -> setTotalUploadCount(activity, uploadCount),
                            t -> Timber.e(t, "Fetching upload count failed")
                    ));
    }

    /**
     * set the sub Title of Contibutions Activity and
     * call function to check for quiz
     * @param uploadCount user's upload count
     */
    private void setTotalUploadCount(Activity activity, int uploadCount) {
        totalUploadCount = uploadCount - revertKvStore.getInt(UPLOAD_SHARED_PREFERENCE, 0);
        if ( totalUploadCount < 0){
            totalUploadCount = 0;
            revertKvStore.putInt(UPLOAD_SHARED_PREFERENCE, 0);
        }
        isUploadCountFetched = true;
        calculateRevertParameter(activity);
    }

    /**
     * To call the API to get reverts count in form of JSONObject
     */
    private void setRevertCount(Activity activity) {
        compositeDisposable.add(okHttpJsonApiClient
                .getAchievements(sessionManager.getUserName())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            response -> {
                                if (response != null) {
                                    setRevertParameter(activity, response.getDeletedUploads());
                                }
                            }, throwable -> Timber.e(throwable, "Fetching feedback failed"))
            );
    }

    /**
     * to calculate the number of images reverted after previous quiz
     * @param revertCountFetched count of deleted uploads
     */
    private void setRevertParameter(Activity activity, int revertCountFetched) {
        revertCount = revertCountFetched - revertKvStore.getInt(REVERT_SHARED_PREFERENCE, 0);
        if (revertCount < 0){
            revertCount = 0;
            revertKvStore.putInt(REVERT_SHARED_PREFERENCE, 0);
        }
        isRevertCountFetched = true;
        calculateRevertParameter(activity);
    }

    /**
     * to check whether the criterion to call quiz is satisfied
     */
    private void calculateRevertParameter(Activity activity) {
        if ( revertCount < 0 || totalUploadCount < 0){
            revertKvStore.putInt(REVERT_SHARED_PREFERENCE, 0);
            revertKvStore.putInt(UPLOAD_SHARED_PREFERENCE, 0);
            return;
        }
        if (isRevertCountFetched && isUploadCountFetched &&
                totalUploadCount >= UPLOAD_COUNT_THRESHOLD &&
                (revertCount * 100) / totalUploadCount >= 50) {
            callQuiz(activity);
        }
    }

    /**
     * Alert which prompts to quiz
     */
    @SuppressLint("StringFormatInvalid")
    private void callQuiz(Activity activity) {
        DialogUtil.showAlertDialog(activity,
                activity.getString(R.string.quiz),
                activity.getString(R.string.quiz_alert_message, REVERT_PERCENTAGE_FOR_MESSAGE),
                activity.getString(R.string.about_translate_proceed),
                activity.getString(android.R.string.cancel),
                () -> startQuizActivity(activity), null);
    }

    private void startQuizActivity(Activity activity) {
        int newRevetSharedPrefs = revertCount + revertKvStore.getInt(REVERT_SHARED_PREFERENCE, 0);
        revertKvStore.putInt(REVERT_SHARED_PREFERENCE, newRevetSharedPrefs);
        int newUploadCount = totalUploadCount + revertKvStore.getInt(UPLOAD_SHARED_PREFERENCE, 0);
        revertKvStore.putInt(UPLOAD_SHARED_PREFERENCE, newUploadCount);
        Intent i = new Intent(activity, WelcomeActivity.class);
        i.putExtra("isQuiz", true);
        activity.startActivity(i);
    }
}
