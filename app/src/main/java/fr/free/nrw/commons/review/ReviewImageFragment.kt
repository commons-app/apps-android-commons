package fr.free.nrw.commons.review

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.auth.csrf.InvalidLoginTokenException
import fr.free.nrw.commons.databinding.FragmentReviewImageBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReviewImageFragment : CommonsDaggerSupportFragment() {

    companion object {
        const val CATEGORY = 2
        private const val SPAM = 0
        private const val COPYRIGHT = 1
        private const val THANKS = 3
    }

    private var position: Int = 0
    private var binding: FragmentReviewImageBinding? = null

    @Inject
    lateinit var sessionManager: SessionManager

    // Constant variable used to store user's key name for onSaveInstanceState method
    private val savedUser = "saved_user"

    // Variable that stores the value of user
    private var user: String? = null

    fun update(position: Int) {
        this.position = position
    }

    private fun updateCategoriesQuestion(): String {
        val media = reviewActivity.media
        if (media?.categoriesHiddenStatus != null && isAdded) {
            // Filter category name attribute from all categories
            val categories = media.categoriesHiddenStatus.keys.map { key ->
                var value = key
                // Each category returned has a format like "Category:<some-category-name>"
                // so remove the prefix "Category:"
                if (key.startsWith("Category:")) {
                    value = key.substring(9)
                }
                value
            }

            val catString = categories.joinToString(", ")
            if (catString.isNotEmpty() && binding?.tvReviewQuestionContext != null) {
                val formattedCatString = "<b>$catString</b>"
                val stringToConvertHtml = getString(
                    R.string.review_category_explanation,
                    formattedCatString
                )
                val formattedString = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    Html.fromHtml(stringToConvertHtml, Html.FROM_HTML_MODE_LEGACY).toString()
                } else {
                    Html.fromHtml(stringToConvertHtml).toString()
                }
                return formattedString
            }
        }
        return getString(R.string.review_no_category)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        position = requireArguments().getInt("position")
        binding = FragmentReviewImageBinding.inflate(inflater, container, false)

        val question: String
        var explanation: String? = null
        val yesButtonText: String
        val noButtonText: String

        binding?.buttonYes?.setOnClickListener { onYesButtonClicked() }

        when (position) {
            SPAM -> {
                question = getString(R.string.review_spam)
                explanation = getString(R.string.review_spam_explanation)
                yesButtonText = getString(R.string.yes)
                noButtonText = getString(R.string.no)
                binding?.buttonNo?.setOnClickListener {
                    reviewActivity.reviewController.reportSpam(requireActivity(), reviewCallback)
                }
            }
            COPYRIGHT -> {
                enableButtons()
                question = getString(R.string.review_copyright)
                explanation = getString(R.string.review_copyright_explanation)
                yesButtonText = getString(R.string.yes)
                noButtonText = getString(R.string.no)
                binding?.buttonNo?.setOnClickListener {
                    reviewActivity.reviewController.reportPossibleCopyRightViolation(
                        requireActivity(),
                        reviewCallback
                    )
                }
            }
            CATEGORY -> {
                enableButtons()
                question = getString(R.string.review_category)
                explanation = updateCategoriesQuestion()
                yesButtonText = getString(R.string.yes)
                noButtonText = getString(R.string.no)
                binding?.buttonNo?.setOnClickListener {
                    reviewActivity.reviewController.reportWrongCategory(
                        requireActivity(),
                        reviewCallback
                    )
                    reviewActivity.swipeToNext()
                }
            }
            THANKS -> {
                enableButtons()
                question = getString(R.string.review_thanks)

                user = reviewActivity.reviewController.firstRevision?.user()
                    ?: savedInstanceState?.getString(savedUser)

                //if the user is null because of whatsoever reason, review will not be sent anyways
                if (!user.isNullOrEmpty()) {
                    explanation = getString(R.string.review_thanks_explanation, user)
                }

                // Note that the yes and no buttons are swapped in this section
                yesButtonText = getString(R.string.review_thanks_yes_button_text)
                noButtonText = getString(R.string.review_thanks_no_button_text)
                binding?.buttonYes?.setTextColor(Color.parseColor("#116aaa"))
                binding?.buttonNo?.setTextColor(Color.parseColor("#228b22"))
                binding?.buttonNo?.setOnClickListener {
                    reviewActivity.reviewController.sendThanks(requireActivity())
                    reviewActivity.swipeToNext()
                }
            }
            else -> {
                enableButtons()
                question = "How did we get here?"
                explanation = "No idea."
                yesButtonText = "yes"
                noButtonText = "no"
            }
        }

        binding?.apply {
            tvReviewQuestion.text = question
            tvReviewQuestionContext.text = explanation
            buttonYes.text = yesButtonText
            buttonNo.text = noButtonText
        }
        return binding?.root
    }

    /**
     * This method will be called when configuration changes happen
     *
     * @param outState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //Save user name when configuration changes happen
        outState.putString(savedUser, user)
    }

    private val reviewCallback: ReviewController.ReviewCallback
        get() = object : ReviewController.ReviewCallback {
            override fun onSuccess() {
                reviewActivity.runRandomizer()
            }

            override fun onFailure() {
                //do nothing
            }

            override fun onTokenException(e: Exception) {
                if (e is InvalidLoginTokenException) {
                    val username = sessionManager.userName
                    val logoutListener = activity?.let {
                        CommonsApplication.BaseLogoutListener(
                            it,
                            getString(R.string.invalid_login_message),
                            username
                        )
                    }

                    if (logoutListener != null) {
                        CommonsApplication.instance.clearApplicationData(
                            requireActivity(), logoutListener
                        )
                    }
                }
            }

            override fun disableButtons() {
                this@ReviewImageFragment.disableButtons()
            }

            override fun enableButtons() {
                this@ReviewImageFragment.enableButtons()
            }
        }

    /**
     * This function is called when an image has
     * been loaded to enable the review buttons.
     */
    fun enableButtons() {
        binding?.apply {
            buttonYes.isEnabled = true
            buttonYes.alpha = 1f
            buttonNo.isEnabled = true
            buttonNo.alpha = 1f
        }
    }

    /**
     * This function is called when an image is being loaded
     * to disable the review buttons
     */
    fun disableButtons() {
        binding?.apply {
            buttonYes.isEnabled = false
            buttonYes.alpha = 0.5f
            buttonNo.isEnabled = false
            buttonNo.alpha = 0.5f
        }
    }

    fun onYesButtonClicked() {
        reviewActivity.swipeToNext()
    }

    private val reviewActivity: ReviewActivity
        get() = requireActivity() as ReviewActivity

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
