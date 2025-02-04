package fr.free.nrw.commons.profile.achievements

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentAchievementsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil.showDismissibleSnackBar
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class AchievementsFragment : CommonsDaggerSupportFragment(){

    @Inject
    lateinit var viewModelFactory: AchievementViewModelFactory
    lateinit var viewModel: AchievementViewModel
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    private var userName: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userName = it.getString(ProfileActivity.KEY_USERNAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(
            this@AchievementsFragment, viewModelFactory)[AchievementViewModel::class.java]
        binding.achievementInfo.setOnClickListener { showInfoDialog() }
        binding.imagesUploadInfoIcon.setOnClickListener { showUploadInfo() }
        binding.imagesRevertedInfoIcon.setOnClickListener { showRevertedInfo() }
        binding.imagesUsedByWikiInfoIcon.setOnClickListener { showUsedByWikiInfo() }
        binding.wikidataEditsIcon.setOnClickListener { showImagesViaNearbyInfo() }
        binding.featuredImageIcon.setOnClickListener { showFeaturedImagesInfo() }
        binding.thanksImageIcon.setOnClickListener { showThanksReceivedInfo() }
        binding.qualityImageIcon.setOnClickListener { showQualityImagesInfo() }

        lifecycleScope.launch {
            viewModel.loading.collectLatest {
                if (it){
                    binding.progressBar.visibility = View.VISIBLE
                } else {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }

        setHasOptionsMenu(true)
        if (sessionManager.userName == null || sessionManager.userName == userName) {
            binding.tvAchievementsOfUser.visibility = View.GONE
        } else {
            binding.tvAchievementsOfUser.visibility = View.VISIBLE
            binding.tvAchievementsOfUser.text = getString(R.string.achievements_of_user, userName)
        }
        if (isBetaFlavour) {
            binding.layout.visibility = View.GONE
            setMenuVisibility(true)
            return binding.root
        }

        setAchievements()
        return binding.root

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun setMenuVisibility(visible: Boolean) {
        super.setMenuVisibility(visible)

        // Whenever this fragment is revealed in a menu,
        // notify Beta users the page data is unavailable
        if (isBetaFlavour && visible) {
            val ctx = context ?: view?.context
            ctx?.let {
                Toast.makeText(it, R.string.achievements_unavailable_beta, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * To invoke the AlertDialog on clicking info button
     */
    fun showInfoDialog() {
        launchAlert(
            resources.getString(R.string.Achievements),
            resources.getString(R.string.achievements_info_message)
        )
    }




    /**
     * To call the API to get results in form Single<JSONObject>
     * which then calls parseJson when results are fetched
     */

    @SuppressLint("SetTextI18n")
    private fun setAchievements() {
        if (checkAccount()) {
            viewModel.getUserAchievements(username = userName.toString())

            lifecycleScope.launch {
                viewModel.achievements.collect{

                    binding.achievementLevel.text = getString(R.string.level,it.level.levelNumber)
                    val store = BasicKvStore(requireContext(), userName)
                    store.putString("userAchievementsLevel", it.level.levelNumber.toString())

                    binding.achievementBadgeImage.setImageDrawable(
                        VectorDrawableCompat.create(
                            resources, R.drawable.badge,
                            ContextThemeWrapper(activity, it.level.levelStyle).theme
                        )
                    )
                    binding.achievementBadgeText.text = it.level.levelNumber.toString()

                    // TODO(use String Format)
                    binding.imageUploadedTVCount.text =
                        it.imagesUploadedCount.toString() + "/" + it.level.maxUploadCount
                    binding.imagesUploadedProgressbar.progress =
                        100 * it.imagesUploadedCount / it.level.maxUploadCount

                    // Revert
                    binding.imageRevertTVCount.text = it.revertedCount.toString() + "%"
                    binding.imageRevertsProgressbar.progress = it.revertedCount
                    binding.imagesRevertLimitText.text =
                        resources.getString(R.string.achievements_revert_limit_message) + it.level.minNonRevertPercentage + "%"

                    // Images Used
                    binding.imagesUsedProgressbar.progress = (100 * it.uniqueImagesCount) / it.level.maxUniqueImages
                    binding.imagesUsedCount.text = (it.uniqueImagesCount.toString() + "/"
                            + it.level.maxUniqueImages)

                    // Thanks Received Badge
                    showBadgesWithCount(view = binding.thanksImageIcon, count =  it.thanksReceivedCount)

                    // Featured Images Badge
                    showBadgesWithCount(view = binding.featuredImageIcon, count =  it.featuredImagesCount)

                    // Quality Images Badge
                    showBadgesWithCount(view = binding.qualityImageIcon, count =  it.qualityImagesCount)

                    showBadgesWithCount(view = binding.wikidataEditsIcon, count = it.imagesEditedBySomeoneElseCount)
                }
            }
        }
    }

    /**
     * Shows a snack bar which has an action button which on click dismisses the snackbar and invokes the
     * listener passed
     * @param tooManyAchievements if this value is true it means that the number of achievements of the
     * user are so high that it wrecks havoc with the Achievements calculator due to which request may time
     * out. Well this is the Ultimate Achievement
     */
    private fun showSnackBarWithRetry(tooManyAchievements: Boolean) {
        if (tooManyAchievements) {
            if (view == null) {
                return
            }
            else {
                binding.progressBar.visibility = View.GONE
                showDismissibleSnackBar(
                    requireView().findViewById(android.R.id.content),
                    R.string.achievements_fetch_failed_ultimate_achievement, R.string.retry
                ) { setAchievements() }
            }

        } else {
            if (view == null) {
                return
            }
            binding.progressBar.visibility = View.GONE
            showDismissibleSnackBar(
                requireView().findViewById(android.R.id.content),
                R.string.achievements_fetch_failed, R.string.retry
            ) { setAchievements() }
        }
    }

    /**
     * Shows a generic error toast when error occurs while loading achievements or uploads
     */
    private fun onError() {
        showLongToast(requireActivity(), resources.getString(R.string.error_occurred))
        binding.progressBar.visibility = View.GONE
    }


    /**
     * used to the uploaded images progressbar
     * @param uploadCount
     */
    private fun setUploadProgress(uploadCount: Int) {
        if (uploadCount == 0) {
            setZeroAchievements()
        } else {
            binding.imagesUploadedProgressbar.visibility = View.VISIBLE
        }
    }

    private fun setZeroAchievements() {
        val message = if (sessionManager.userName != userName) {
            getString(R.string.no_achievements_yet, userName )
        } else {
            getString(R.string.you_have_no_achievements_yet)
        }
        showAlertDialog(
            requireActivity(),
            null,
            message,
            getString(R.string.ok),
            {}
        )
        binding.layout.visibility = View.INVISIBLE
//        binding.imagesUploadedProgressbar.setVisibility(View.INVISIBLE);
//        binding.imageRevertsProgressbar.setVisibility(View.INVISIBLE);
//        binding.imagesUsedByWikiProgressBar.setVisibility(View.INVISIBLE);
        //binding.achievementBadgeImage.visibility = View.INVISIBLE // TODO
        binding.imagesUsedCount.setText(R.string.no_image)
        binding.imagesRevertedText.setText(R.string.no_image_reverted)
        binding.imagesUploadTextParam.setText(R.string.no_image_uploaded)
    }

    /**
     * This function is used to show badge on any view (button, imageView, etc)
     * @param view The View on which the badge will be displayed eg (button, imageView, etc)
     * @param count The number to be displayed inside the badge.
     * @param backgroundColor The badge background color. Default is R.attr.colorPrimary
     * @param badgeTextColor The badge text color. Default is R.attr.colorPrimary
     * @param badgeGravity The position of the badge [TOP_END,TOP_START,BOTTOM_END,BOTTOM_START]. Default is TOP_END
     * @return if the number is 0, then it will not create badge for it and hide the view
     * @see https://developer.android.com/reference/com/google/android/material/badge/BadgeDrawable
     */

    private fun showBadgesWithCount(
        view: View,
        count: Int,
        backgroundColor: Int = R.attr.colorPrimary,
        badgeTextColor: Int = R.attr.textEnabled,
        badgeGravity: Int = BadgeDrawable.TOP_END
    ) {
        //https://stackoverflow.com/a/67742035
        if (count == 0) {
            view.visibility = View.GONE
            return
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            /**
             * Callback method to be invoked when the global layout state or the visibility of views
             * within the view tree changes
             */
            @ExperimentalBadgeUtils
            override fun onGlobalLayout() {
                view.visibility = View.VISIBLE
                val badgeDrawable = BadgeDrawable.create(requireActivity())
                badgeDrawable.number = count
                badgeDrawable.badgeGravity = badgeGravity
                badgeDrawable.badgeTextColor = badgeTextColor
                badgeDrawable.backgroundColor = backgroundColor
                BadgeUtils.attachBadgeDrawable(badgeDrawable, view)
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
            }
        })
    }


    fun showUploadInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.images_uploaded),
            resources.getString(R.string.images_uploaded_explanation),
            IMAGES_UPLOADED_URL
        )
    }

    fun showRevertedInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.image_reverts),
            resources.getString(R.string.images_reverted_explanation),
            IMAGES_REVERT_URL
        )
    }

    fun showUsedByWikiInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.images_used_by_wiki),
            resources.getString(R.string.images_used_explanation),
            IMAGES_USED_URL
        )
    }

    fun showImagesViaNearbyInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.statistics_wikidata_edits),
            resources.getString(R.string.images_via_nearby_explanation),
            IMAGES_NEARBY_PLACES_URL
        )
    }

    fun showFeaturedImagesInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.statistics_featured),
            resources.getString(R.string.images_featured_explanation),
            IMAGES_FEATURED_URL
        )
    }

    fun showThanksReceivedInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.statistics_thanks),
            resources.getString(R.string.thanks_received_explanation),
            THANKS_URL
        )
    }

    fun showQualityImagesInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.statistics_quality),
            resources.getString(R.string.quality_images_info),
            QUALITY_IMAGE_URL
        )
    }

    /**
     * takes title and message as input to display alerts
     * @param title
     * @param message
     */
    private fun launchAlert(title: String, message: String) {
        showAlertDialog(
            requireActivity(),
            title,
            message,
            getString(R.string.ok),
            {}
        )
    }

    /**
     * Launch Alert with a READ MORE button and clicking it open a custom webpage
     */
    private fun launchAlertWithHelpLink(title: String, message: String, helpLinkUrl: String) {
        showAlertDialog(
            requireActivity(),
            title,
            message,
            getString(R.string.ok),
            getString(R.string.read_help_link),
            {},
            { Utils.handleWebUrl(requireContext(), Uri.parse(helpLinkUrl)) },
            null
        )
    }
    /**
     * check to ensure that user is logged in
     * @return
     */
    private fun checkAccount(): Boolean {
        val currentAccount = sessionManager.currentAccount
        if (currentAccount == null) {
            Timber.d("Current account is null")
            showLongToast(requireActivity(), resources.getString(R.string.user_not_logged_in))
            sessionManager.forceLogin(activity)
            return false
        }
        return true
    }



    companion object{
        /**
         * Help link URLs
         */
        private const val IMAGES_UPLOADED_URL = "https://commons.wikimedia.org/wiki/Commons:Project_scope"
        private const val IMAGES_REVERT_URL = "https://commons.wikimedia.org/wiki/Commons:Deletion_policy#Reasons_for_deletion"
        private const val IMAGES_USED_URL = "https://en.wikipedia.org/wiki/Wikipedia:Manual_of_Style/Images"
        private const val IMAGES_NEARBY_PLACES_URL = "https://www.wikidata.org/wiki/Property:P18"
        private const val IMAGES_FEATURED_URL = "https://commons.wikimedia.org/wiki/Commons:Featured_pictures"
        private const val QUALITY_IMAGE_URL = "https://commons.wikimedia.org/wiki/Commons:Quality_images"
        private const val THANKS_URL = "https://www.mediawiki.org/wiki/Extension:Thanks"
    }
}