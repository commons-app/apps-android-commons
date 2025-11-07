package fr.free.nrw.commons.navtab

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import fr.free.nrw.commons.AboutActivity
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.CommonsApplication.ActivityLogoutListener
import fr.free.nrw.commons.R
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.databinding.FragmentMoreBottomSheetBinding
import fr.free.nrw.commons.feedback.FeedbackContentCreator
import fr.free.nrw.commons.feedback.FeedbackDialog
import fr.free.nrw.commons.feedback.OnFeedbackSubmitCallback
import fr.free.nrw.commons.feedback.model.Feedback
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.logging.CommonsLogSender
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.review.ReviewActivity
import fr.free.nrw.commons.settings.SettingsActivity
import fr.free.nrw.commons.startWelcome
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MoreBottomSheetFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var commonsLogSender: CommonsLogSender

    @Inject
    @field: Named("default_preferences")
    lateinit var store: JsonKvStore

    @Inject
    @field: Named("commons-page-edit")
    lateinit var pageEditClient: PageEditClient

    companion object {
        private const val GITHUB_ISSUES_URL =
            "https://github.com/commons-app/apps-android-commons/issues"
    }

    private var binding: FragmentMoreBottomSheetBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMoreBottomSheetBinding.inflate(inflater, container, false)

        if (store.getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED)) {
            binding?.morePeerReview?.visibility = View.GONE
        }

        binding?.apply {
            moreLogout.setOnClickListener { onLogoutClicked() }
            moreFeedback.setOnClickListener { onFeedbackClicked() }
            moreAbout.setOnClickListener { onAboutClicked() }
            moreTutorial.setOnClickListener { onTutorialClicked() }
            moreSettings.setOnClickListener { onSettingsClicked() }
            moreProfile.setOnClickListener { onProfileClicked() }
            morePeerReview.setOnClickListener { onPeerReviewClicked() }
            moreFeedbackGithub.setOnClickListener { onFeedbackGithubClicked() }
        }

        setUserName()
        return binding?.root
    }

    private fun onFeedbackGithubClicked() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(GITHUB_ISSUES_URL)
        }
        startActivity(intent)
    }

    // Hilt automatically injects dependencies for @AndroidEntryPoint annotated fragments
    // No manual onAttach injection needed

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    /**
     * Set the username and user achievements level (if available) in navigationHeader.
     */
    private fun setUserName() {
        val store = BasicKvStore(requireContext(), getUserName())
        val level = store.getString("userAchievementsLevel", "0")
        if (level == "0"){
            binding?.moreProfile?.text = getString(
                R.string.profile_withoutLevel,
                getUserName(),
                getString(R.string.see_your_achievements) // Second argument
            )
        } else {
            binding?.moreProfile?.text = getString(
                R.string.profile_withLevel,
                getUserName(),
                level
            )
        }
    }

    private fun getUserName(): String {
        val accountManager = AccountManager.get(requireActivity())
        val allAccounts = accountManager.getAccountsByType(BuildConfig.ACCOUNT_TYPE)
        return if (allAccounts.isNotEmpty()) {
            allAccounts[0].name
        } else {
            ""
        }
    }

    fun onLogoutClicked() {
        AlertDialog.Builder(requireActivity())
            .setMessage(R.string.logout_verification)
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { _, _ ->
                val app = requireContext().applicationContext as CommonsApplication
                app.clearApplicationData(requireContext(), ActivityLogoutListener(requireActivity(), requireContext()))
            }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.cancel() }
            .show()
    }

    fun onFeedbackClicked() {
        showFeedbackDialog()
    }

    /**
     * Creates and shows a dialog asking feedback from users
     */
    private fun showFeedbackDialog() {
        FeedbackDialog(requireContext(), object : OnFeedbackSubmitCallback{
            override fun onFeedbackSubmit(feedback: Feedback) {
                uploadFeedback(feedback)
            }
        }).apply {
            setCancelable(false)
            show()
        }
    }

    /**
     * Uploads feedback data on the server
     */
    @SuppressLint("CheckResult")
    fun uploadFeedback(feedback: Feedback) {
        val feedbackContentCreator = FeedbackContentCreator(requireContext(), feedback)

        val single = pageEditClient.createNewSection(
            "Commons:Mobile_app/Feedback",
            feedbackContentCreator.getSectionTitle(),
            feedbackContentCreator.getSectionText(),
            "New feedback on version ${feedback.version} of the app"
        )
            .flatMapSingle { Single.just(it) }
            .firstOrError()

        Single.defer { single }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ success ->
                val messageResId = if (success) {
                    R.string.thanks_feedback
                } else {
                    R.string.error_feedback
                }
                Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show()
            }, { error ->
                Timber.e(error)
                Toast.makeText(requireContext(), R.string.error_feedback, Toast.LENGTH_SHORT).show()
            })
    }

    /**
     * This method shows the alert dialog when a user wants to send feedback about the app.
     */
    private fun showAlertDialog() {
        AlertDialog.Builder(requireActivity())
            .setMessage(R.string.feedback_sharing_data_alert)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ -> sendFeedback() }
            .show()
    }

    /**
     * This method collects the feedback message and starts the activity with implicit intent
     * to the available email client.
     */
    @SuppressLint("IntentReset")
    private fun sendFeedback() {
        val technicalInfo = commonsLogSender.getExtraInfo()

        val feedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
            type = "message/rfc822"
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(CommonsApplication.FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, CommonsApplication.FEEDBACK_EMAIL_SUBJECT)
            putExtra(Intent.EXTRA_TEXT, "\n\n${CommonsApplication.FEEDBACK_EMAIL_TEMPLATE_HEADER}\n$technicalInfo")
        }

        try {
            startActivity(feedbackIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.no_email_client, Toast.LENGTH_SHORT).show()
        }
    }

    fun onAboutClicked() {
        val intent = Intent(activity, AboutActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        requireActivity().startActivity(intent)
    }

    fun onTutorialClicked() {
        requireContext().startWelcome()
    }

    fun onSettingsClicked() {
        val intent = Intent(activity, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        requireActivity().startActivity(intent)
    }

    fun onProfileClicked() {
        ProfileActivity.startYourself(requireActivity(), getUserName(), false)
    }

    fun onPeerReviewClicked() {
        ReviewActivity.startYourself(requireActivity(), getString(R.string.title_activity_review))
    }
}