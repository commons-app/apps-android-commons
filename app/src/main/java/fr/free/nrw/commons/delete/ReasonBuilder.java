package fr.free.nrw.commons.delete;

import android.accounts.Account;
import android.content.Context;

import org.wikipedia.util.DateUtil;

import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.achievements.FeedbackResponse;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Single;
import timber.log.Timber;

@Singleton
public class ReasonBuilder {

    private SessionManager sessionManager;
    private OkHttpJsonApiClient okHttpJsonApiClient;
    private Context context;

    @Inject
    public ReasonBuilder(Context context,
                         SessionManager sessionManager,
                         OkHttpJsonApiClient okHttpJsonApiClient) {
        this.context = context;
        this.sessionManager = sessionManager;
        this.okHttpJsonApiClient = okHttpJsonApiClient;
    }

    private String prettyUploadedDate(Media media) {
        Date date = media.getDateUploaded();
        if (date == null || date.toString() == null || date.toString().isEmpty()) {
            return "Uploaded date not available";
        }
        return DateUtil.getDateStringWithSkeletonPattern(date,"dd MMM yyyy");
    }

    private Single<String> fetchArticleNumber(Media media, String reason) {
        if (checkAccount()) {
            return okHttpJsonApiClient
                    .getAchievements(sessionManager.getCurrentAccount().name)
                    .map(feedbackResponse -> appendArticlesUsed(feedbackResponse, media, reason));
        }
        return Single.just("");
    }

    private String appendArticlesUsed(FeedbackResponse object, Media media, String reason) {
        String reason1Template = context.getString(R.string.uploaded_by_myself);
        reason += String.format(Locale.getDefault(), reason1Template, prettyUploadedDate(media), object.getArticlesUsingImages());
        Timber.i("New Reason %s", reason);
        return reason;
    }


    public Single<String> getReason(Media media, String reason) {
        return fetchArticleNumber(media, reason);
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
