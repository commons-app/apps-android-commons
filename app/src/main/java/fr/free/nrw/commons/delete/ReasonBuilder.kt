package fr.free.nrw.commons.delete

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log

import fr.free.nrw.commons.utils.DateUtil
import java.util.Locale

import javax.inject.Inject
import javax.inject.Singleton

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.profile.achievements.FeedbackResponse
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.utils.ViewUtilWrapper
import io.reactivex.Single
import timber.log.Timber

/**
 * This class handles the reason for deleting a Media object
 */
@Singleton
class ReasonBuilder @Inject constructor(
    private val context: Context,
    private val sessionManager: SessionManager,
    private val okHttpJsonApiClient: OkHttpJsonApiClient,
    private val viewUtilWrapper: ViewUtilWrapper
) {

    /**
     * To process the reason and append the media's upload date and uploaded_by_me string
     * @param media
     * @param reason
     * @return
     */
    fun getReason(media: Media?, reason: String?): Single<String> {
        if (media == null || reason == null) {
            return Single.just("Not known")
        }
        Timber.d("Fetching article number")
        return fetchArticleNumber(media, reason)
    }

    /**
     * get upload date for the passed Media
     */
    private fun prettyUploadedDate(media: Media): String {
        val date = media.dateUploaded
        return if (date == null || date.toString().isEmpty()) {
            "Uploaded date not available"
        } else {
            DateUtil.getDateStringWithSkeletonPattern(date, "dd MMM yyyy")
        }
    }

    private fun fetchArticleNumber(media: Media, reason: String): Single<String> {
        return if (checkAccount()) {
            Timber.d("Fetching achievements for ${sessionManager.userName}")
            okHttpJsonApiClient
                .getAchievements(sessionManager.userName)
                .map { feedbackResponse ->
                    Timber.d("Feedback Response: $feedbackResponse")
                    appendArticlesUsed(feedbackResponse, media, reason)
                }
        } else {
            Single.just("")
        }
    }

    /**
     * Takes the uploaded_by_me string, the upload date, name of articles using images
     * and appends it to the received reason
     * @param feedBack object
     * @param media whose upload data is to be fetched
     * @param reason
     */
    @SuppressLint("StringFormatInvalid")
    private fun appendArticlesUsed(feedBack: FeedbackResponse, media: Media, reason: String): String {
        val reason1Template = context.getString(R.string.uploaded_by_myself)
        return reason + String.format(Locale.getDefault(), reason1Template, prettyUploadedDate(media), feedBack.articlesUsingImages)
            .also { Timber.i("New Reason %s", it) }
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private fun checkAccount(): Boolean {
        return if (!sessionManager.doesAccountExist()) {
            Timber.d("Current account is null")
            viewUtilWrapper.showLongToast(context, context.getString(R.string.user_not_logged_in))
            sessionManager.forceLogin(context)
            false
        } else {
            true
        }
    }
}
