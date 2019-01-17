package fr.free.nrw.commons.delete;

import android.accounts.Account;
import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ReasonBuilder {

    private SessionManager sessionManager;
    private MediaWikiApi mediaWikiApi;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private String reason;
    private Context context;
    private Media media;

    public ReasonBuilder(String reason,
                         Context context,
                         Media media,
                         SessionManager sessionManager,
                         MediaWikiApi mediaWikiApi){
        this.reason = reason;
        this.context = context;
        this.media = media;
        this.sessionManager = sessionManager;
        this.mediaWikiApi = mediaWikiApi;
    }

    private String prettyUploadedDate(Media media) {
        Date date = media.getDateUploaded();
        if (date == null || date.toString() == null || date.toString().isEmpty()) {
            return "Uploaded date not available";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return formatter.format(date);
    }

    private void fetchArticleNumber() {
        if (checkAccount()) {
            compositeDisposable.add(mediaWikiApi
                    .getAchievements(sessionManager.getCurrentAccount().name)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            this::appendArticlesUsed,
                            t -> Timber.e(t, "Fetching achievements statistics failed")
                    ));
        }
    }

    private void appendArticlesUsed(FeedbackResponse object){
        reason += context.getString(R.string.uploaded_by_myself).toString() + prettyUploadedDate(media);
        reason += context.getString(R.string.used_by).toString()
                + object.getArticlesUsingImages()
                + context.getString(R.string.articles).toString();
        Log.i("New Reason", reason);
    }


    public String getReason(){
        fetchArticleNumber();
        return reason;
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private boolean checkAccount(){
        Account currentAccount = sessionManager.getCurrentAccount();
        if(currentAccount == null) {
            Timber.d("Current account is null");
            ViewUtil.showLongToast(context, context.getResources().getString(R.string.user_not_logged_in));
            sessionManager.forceLogin(context);
            return false;
        }
        return true;
    }
}
