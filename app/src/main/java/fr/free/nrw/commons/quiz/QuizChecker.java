package fr.free.nrw.commons.quiz;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog.Builder;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
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
public class QuizChecker {

    private int revertCount ;
    private int totalUploadCount ;
    private boolean isRevertCountFetched;
    private boolean isUploadCountFetched;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    public Context context;
    private String userName;
    private MediaWikiApi mediaWikiApi;
    private SharedPreferences revertPref;
    private SharedPreferences countPref;

    private static final int UPLOAD_COUNT_THRESHOLD = 5;
    private static final String REVERT_PERCENTAGE_FOR_MESSAGE = "50%";
    private final String REVERT_SHARED_PREFERENCE = "revertCount";
    private final String UPLOAD_SHARED_PREFERENCE = "uploadCount";

    /**
     * constructor to set the parameters for quiz
     * @param context context
     * @param userName Commons user name
     * @param mediaWikiApi instance of MediaWikiApi
     */
    public QuizChecker(Context context, String userName, MediaWikiApi mediaWikiApi) {
        this.context = context;
        this.userName = userName;
        this.mediaWikiApi = mediaWikiApi;
        revertPref = context.getSharedPreferences(REVERT_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        countPref = context.getSharedPreferences(UPLOAD_SHARED_PREFERENCE,Context.MODE_PRIVATE);
        setUploadCount();
        setRevertCount();
    }

    /**
     * to fet the total number of images uploaded
     */
    private void setUploadCount() {
            compositeDisposable.add(mediaWikiApi
                    .getUploadCount(userName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::setTotalUploadCount,
                            t -> Timber.e(t, "Fetching upload count failed")
                    ));
    }

    /**
     * set the sub Title of Contibutions Activity and
     * call function to check for quiz
     * @param uploadCount user's upload count
     */
    private void setTotalUploadCount(int uploadCount) {
        totalUploadCount = uploadCount - countPref.getInt(UPLOAD_SHARED_PREFERENCE,0);
        if ( totalUploadCount < 0){
            totalUploadCount = 0;
            countPref.edit().putInt(UPLOAD_SHARED_PREFERENCE,0).apply();
        }
        isUploadCountFetched = true;
        calculateRevertParameter();
    }

    /**
     * To call the API to get reverts count in form of JSONObject
     */
    private void setRevertCount() {
            compositeDisposable.add(mediaWikiApi
                    .getAchievements(userName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            response -> {
                                if (response != null) {
                                    setRevertParameter(response.getDeletedUploads());
                                }
                            }, throwable -> Timber.e(throwable, "Fetching feedback failed"))
            );
    }

    /**
     * to calculate the number of images reverted after previous quiz
     * @param revertCountFetched count of deleted uploads
     */
    private void setRevertParameter(int revertCountFetched) {
        revertCount = revertCountFetched - revertPref.getInt(REVERT_SHARED_PREFERENCE,0);
        if (revertCount < 0){
            revertCount = 0;
            revertPref.edit().putInt(REVERT_SHARED_PREFERENCE, 0).apply();
        }
        isRevertCountFetched = true;
        calculateRevertParameter();
    }

    /**
     * to check whether the criterion to call quiz is satisfied
     */
    private void calculateRevertParameter() {
        if ( revertCount < 0 || totalUploadCount < 0){
            revertPref.edit().putInt(REVERT_SHARED_PREFERENCE, 0).apply();
            countPref.edit().putInt(UPLOAD_SHARED_PREFERENCE,0).apply();
            return;
        }
        if (isRevertCountFetched && isUploadCountFetched &&
                totalUploadCount >= UPLOAD_COUNT_THRESHOLD &&
                (revertCount * 100) / totalUploadCount >= 50) {
            callQuiz();
        }
    }

    /**
     * Alert which prompts to quiz
     */
    private void callQuiz() {
        Builder alert = new Builder(context);
        alert.setTitle(context.getResources().getString(R.string.quiz));
        alert.setMessage(context.getResources().getString(R.string.quiz_alert_message,
                REVERT_PERCENTAGE_FOR_MESSAGE));
        alert.setPositiveButton(R.string.about_translate_proceed, (dialog, which) -> {
            int newRevetSharedPrefs = revertCount + revertPref.getInt(REVERT_SHARED_PREFERENCE, 0);
            revertPref.edit().putInt(REVERT_SHARED_PREFERENCE, newRevetSharedPrefs).apply();
            int newUploadCount = totalUploadCount + countPref.getInt(UPLOAD_SHARED_PREFERENCE, 0);
            countPref.edit().putInt(UPLOAD_SHARED_PREFERENCE, newUploadCount).apply();
            Intent i = new Intent(context, WelcomeActivity.class);
            i.putExtra("isQuiz", true);
            dialog.dismiss();
            context.startActivity(i);
        });
        alert.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel());
        android.support.v7.app.AlertDialog dialog = alert.create();
        dialog.show();
    }
}
