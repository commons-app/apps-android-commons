package fr.free.nrw.commons.quiz

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import fr.free.nrw.commons.R
import fr.free.nrw.commons.WelcomeActivity
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.utils.DialogUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


/**
 * Fetches the number of images uploaded and number of images reverted.
 * Then it calculates the percentage of the images reverted.
 * If the percentage of images reverted after the last quiz exceeds 50% and number of images uploaded is
 * greater than 50, then the quiz is popped up.
 */
@Singleton
class QuizChecker @Inject constructor(
    private val sessionManager: SessionManager,
    private val okHttpJsonApiClient: OkHttpJsonApiClient,
    @Named("default_preferences") private val revertKvStore: JsonKvStore
) {

    private var revertCount = 0
    private var totalUploadCount = 0
    private var isRevertCountFetched = false
    private var isUploadCountFetched = false

    private val compositeDisposable = CompositeDisposable()

    private val uploadCountThreshold = 5
    private val revertPercentageForMessage = "50%"
    private val revertSharedPreference = "revertCount"
    private val uploadSharedPreference = "uploadCount"

    /**
     * Initializes quiz check by calculating revert parameters and showing quiz if necessary
     */
    fun initQuizCheck(activity: Activity) {
        calculateRevertParameterAndShowQuiz(activity)
    }

    /**
     * Clears disposables to avoid memory leaks
     */
    fun cleanup() {
        compositeDisposable.clear()
    }

    /**
     * Fetches the total number of images uploaded
     */
    private fun setUploadCount() {
        compositeDisposable.add(
            okHttpJsonApiClient.getUploadCount(sessionManager.userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { uploadCount -> setTotalUploadCount(uploadCount) },
                    { t -> Timber.e(t, "Fetching upload count failed") }
                )
        )
    }

    /**
     * Sets the total upload count after subtracting stored preference
     * @param uploadCount User's upload count
     */
    private fun setTotalUploadCount(uploadCount: Int) {
        totalUploadCount = uploadCount - revertKvStore.getInt(
            uploadSharedPreference,
            0
        )
        if (totalUploadCount < 0) {
            totalUploadCount = 0
            revertKvStore.putInt(uploadSharedPreference, 0)
        }
        isUploadCountFetched = true
    }

    /**
     * Fetches the revert count using the API
     */
    private fun setRevertCount() {
        compositeDisposable.add(
            okHttpJsonApiClient.getAchievements(sessionManager.userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { response ->
                        response?.let { setRevertParameter(it.deletedUploads) }
                    },
                    { throwable -> Timber.e(throwable, "Fetching feedback failed") }
                )
        )
    }

    /**
     * Calculates the number of images reverted after the previous quiz
     * @param revertCountFetched Count of deleted uploads
     */
    private fun setRevertParameter(revertCountFetched: Int) {
        revertCount = revertCountFetched - revertKvStore.getInt(revertSharedPreference, 0)
        if (revertCount < 0) {
            revertCount = 0
            revertKvStore.putInt(revertSharedPreference, 0)
        }
        isRevertCountFetched = true
    }

    /**
     * Checks whether the criteria for calling the quiz are satisfied
     */
    private fun calculateRevertParameterAndShowQuiz(activity: Activity) {
        setUploadCount()
        setRevertCount()

        if (revertCount < 0 || totalUploadCount < 0) {
            revertKvStore.putInt(revertSharedPreference, 0)
            revertKvStore.putInt(uploadSharedPreference, 0)
            return
        }

        if (isRevertCountFetched && isUploadCountFetched &&
            totalUploadCount >= uploadCountThreshold &&
            (revertCount * 100) / totalUploadCount >= 50
        ) {
            callQuiz(activity)
        }
    }

    /**
     * Displays an alert prompting the user to take the quiz
     */
    @SuppressLint("StringFormatInvalid")
    private fun callQuiz(activity: Activity) {
        DialogUtil.showAlertDialog(
            activity,
            activity.getString(R.string.quiz),
            activity.getString(R.string.quiz_alert_message, revertPercentageForMessage),
            activity.getString(R.string.about_translate_proceed),
            activity.getString(android.R.string.cancel),
            { startQuizActivity(activity) },
            null
        )
    }

    /**
     * Starts the quiz activity and updates preferences for revert and upload counts
     */
    private fun startQuizActivity(activity: Activity) {
        val newRevertSharedPrefs = revertCount + revertKvStore.getInt(revertSharedPreference, 0)
        revertKvStore.putInt(revertSharedPreference, newRevertSharedPrefs)

        val newUploadCount = totalUploadCount + revertKvStore.getInt(uploadSharedPreference, 0)
        revertKvStore.putInt(uploadSharedPreference, newUploadCount)

        val intent = Intent(activity, WelcomeActivity::class.java).apply {
            putExtra("isQuiz", true)
        }
        activity.startActivity(intent)
    }
}
