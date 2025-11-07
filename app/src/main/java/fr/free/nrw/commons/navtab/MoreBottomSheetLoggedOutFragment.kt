package fr.free.nrw.commons.navtab

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
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
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.databinding.FragmentMoreBottomSheetLoggedOutBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.logging.CommonsLogSender
import fr.free.nrw.commons.settings.SettingsActivity
import javax.inject.Inject
import javax.inject.Named
import timber.log.Timber

@AndroidEntryPoint
class MoreBottomSheetLoggedOutFragment : BottomSheetDialogFragment() {

    private var binding: FragmentMoreBottomSheetLoggedOutBinding? = null

    @Inject
    lateinit var commonsLogSender: CommonsLogSender

    @Inject
    @field: Named("default_preferences")
    lateinit var applicationKvStore: JsonKvStore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMoreBottomSheetLoggedOutBinding.inflate(
            inflater,
            container,
            false
        )
        return binding?.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        binding?.apply {
            moreLogin.setOnClickListener { onLogoutClicked() }
            moreFeedback.setOnClickListener { onFeedbackClicked() }
            moreAbout.setOnClickListener { onAboutClicked() }
            moreSettings.setOnClickListener { onSettingsClicked() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    // Hilt automatically injects dependencies for @AndroidEntryPoint annotated fragments
    // No manual onAttach injection needed

    fun onLogoutClicked() {
        applicationKvStore.putBoolean("login_skipped", false)
        val intent = Intent(context, LoginActivity::class.java)
        requireActivity().finish() // Kill the activity from which you will go to next activity
        startActivity(intent)
    }

    fun onFeedbackClicked() {
        showAlertDialog()
    }

    /**
     * This method shows the alert dialog when a user wants to send feedback about the app.
     */
    private fun showAlertDialog() {
        AlertDialog.Builder(requireActivity())
            .setMessage(R.string.feedback_sharing_data_alert)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ -> sendFeedback() }
            .setNegativeButton(R.string.cancel){_,_ -> }
            .show()
    }

    /**
     * This method collects the feedback message and starts an activity with an implicit intent to
     * the available email client.
     */
    @SuppressLint("IntentReset")
    private fun sendFeedback() {
        val technicalInfo = commonsLogSender.getExtraInfo()

        val feedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
            type = "message/rfc822"
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(CommonsApplication.FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, CommonsApplication.FEEDBACK_EMAIL_SUBJECT)
            putExtra(
                Intent.EXTRA_TEXT,
                "\n\n${CommonsApplication.FEEDBACK_EMAIL_TEMPLATE_HEADER}\n$technicalInfo"
            )
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

    fun onSettingsClicked() {
        val intent = Intent(activity, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        requireActivity().startActivity(intent)
    }

    private inner class BaseLogoutListener : CommonsApplication.LogoutListener {

        override fun onLogoutComplete() {
            Timber.d("Logout complete callback received.")
            val nearbyIntent = Intent(context, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(nearbyIntent)
            requireActivity().finish()
        }
    }
}