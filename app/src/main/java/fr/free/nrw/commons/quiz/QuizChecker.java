package fr.free.nrw.commons.quiz;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.di.CommonsDaggerContentProvider;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * to check whether to pop quiz or not
 */
public class QuizChecker {

    private int revertCount;
    private int totalUploadCount;
    private boolean isRevertCountFetched;
    private boolean isUploadCountFetched;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    public Context context;
    private String userName;
    private MediaWikiApi mediaWikiApi;
    private SharedPreferences revertPref;
    private SharedPreferences countPref;

    /**
     * constructor to set the parameters for quiz
     * @param context
     * @param userName
     * @param mediaWikiApi
     */
    public QuizChecker( Context context, String userName, MediaWikiApi mediaWikiApi){
        this.context = context;
        this.userName = userName;
        this.mediaWikiApi = mediaWikiApi;
        revertPref = context.getSharedPreferences("revertCount", Context.MODE_PRIVATE);
        countPref = context.getSharedPreferences("uploadCount",Context.MODE_PRIVATE);
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
                    .subscribe(
                            uploadCount -> setTotalUploadCount(uploadCount),
                            t -> Timber.e(t, "Fetching upload count failed")
                    ));
    }

    /**
     * set the sub Title of Contibutions Activity and
     * call function to check for quiz
     * @param uploadCount
     */
    private void setTotalUploadCount( int uploadCount){
        totalUploadCount = uploadCount - countPref.getInt("uploadCount",0);
        isUploadCountFetched = true;
        calculateRevertParamater();
    }

    /**
     * To call the API to get reverts count in form of JSONObject
     *
     */
    private void setRevertCount(){
            compositeDisposable.add(mediaWikiApi
                    .getRevertCount(userName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            object -> setRevertParamter(object.getInt("deletedUploads"))
                    ));

    }

    /**
     * to calculate the number of images reverted after previous quiz
     * @param revertCountFetched
     */
    private void setRevertParamter( int revertCountFetched){
        revertCount = revertCountFetched - revertPref.getInt("revertCount",0);
        isRevertCountFetched = true;
        calculateRevertParamater();
    }

    /**
     * to check whether the criterion to call quiz is satisfied
     */
    private void calculateRevertParamater(){
        if(isRevertCountFetched && isUploadCountFetched && totalUploadCount >= 5){
            if( (revertCount * 100)/totalUploadCount >= 50){
                callQuiz();
            }
        }
    }

    /**
     * Alert which prompts to quiz
     */
    public void callQuiz(){
        Builder alert = new Builder(context);
        alert.setTitle(context.getResources().getString(R.string.quiz));
        alert.setMessage(context.getResources().getString(R.string.quiz_alert_message));
        alert.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               int newRevetSharedPrefs = revertCount+ revertPref.getInt("revertCount",0);
                revertPref.edit().putInt("revertCount", newRevetSharedPrefs).apply();
                int newUploadCount = totalUploadCount + countPref.getInt("uploadCount",0);
                countPref.edit().putInt("uploadCount",newUploadCount).apply();
                Intent i = new Intent(context, WelcomeActivity.class);
                i.putExtra("isQuiz", true);
                context.startActivity(i);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        android.support.v7.app.AlertDialog dialog = alert.create();
        dialog.show();
    }

}
