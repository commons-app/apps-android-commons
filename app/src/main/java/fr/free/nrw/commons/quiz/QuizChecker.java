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
    private int revertCounter;
    private SharedPreferences revertPref;

    public QuizChecker( Context context, String userName, MediaWikiApi mediaWikiApi){
        this.context = context;
        this.userName = userName;
        this.mediaWikiApi = mediaWikiApi;
        revertPref = context.getSharedPreferences("revertCount", Context.MODE_PRIVATE);
        setUploadCount();
        setRevertCount();
    }

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
        totalUploadCount = uploadCount;
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

    private void setRevertParamter( int revertCountFetched){
        revertCount = revertCountFetched - revertPref.getInt("revertCount",0);
        isRevertCountFetched = true;
        calculateRevertParamater();
    }

    private void calculateRevertParamater(){
        callQuiz();
        if(isRevertCountFetched && isUploadCountFetched && totalUploadCount != 0){
            if( (revertCount * 100)/totalUploadCount >= 50){
                callQuiz();
            }
        }
    }
    public void callQuiz(){
        Builder alert = new Builder(context);
        alert.setTitle(context.getResources().getString(R.string.quiz));
        alert.setMessage(context.getResources().getString(R.string.warning_for_image_reverts));
        alert.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               int newRevetSharedPrefs = revertCount+ revertPref.getInt("revertCount",0);
                revertPref.edit().putInt("revertCount", newRevetSharedPrefs).apply();
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
