package fr.free.nrw.commons.profile.achievements

import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentAchievementsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.profile.achievements.LevelController.LevelInfo.Companion.from
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil.showDismissibleSnackBar
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.util.Objects
import javax.inject.Inject

class AchievementsFragment : CommonsDaggerSupportFragment(){
    private lateinit var levelInfo: LevelController.LevelInfo

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
//    private lateinit var binding: FragmentAchievementsBinding
    private val compositeDisposable = CompositeDisposable()

    // To keep track of the number of wiki edits made by a user
    private var numberOfEdits: Int = 0

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

        binding.achievementInfo.setOnClickListener { showInfoDialog() }
        binding.imagesUploadInfo.setOnClickListener { showUploadInfo() }
        binding.imagesRevertedInfo.setOnClickListener { showRevertedInfo() }
        binding.imagesUsedByWikiInfo.setOnClickListener { showUsedByWikiInfo() }
        binding.imagesNearbyInfo.setOnClickListener { showImagesViaNearbyInfo() }
        binding.imagesFeaturedInfo.setOnClickListener { showFeaturedImagesInfo() }
        binding.thanksReceivedInfo.setOnClickListener { showThanksReceivedInfo() }
        binding.qualityImagesInfo.setOnClickListener { showQualityImagesInfo() }

        // DisplayMetrics used to fetch the size of the screen
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        // Used for the setting the size of imageView at runtime
        val params = binding.achievementBadgeImage.layoutParams as ConstraintLayout.LayoutParams
        params.height = (height * BADGE_IMAGE_HEIGHT_RATIO).toInt()
        params.width = (width * BADGE_IMAGE_WIDTH_RATIO).toInt()
        binding.achievementBadgeImage.requestLayout()
        binding.progressBar.visibility = View.VISIBLE

        setHasOptionsMenu(true)

        // Set the initial value of WikiData edits to 0
        binding.wikidataEdits.text = "0"
        if (sessionManager.userName == null || sessionManager.userName == userName) {
            binding.tvAchievementsOfUser.visibility = View.GONE
        } else {
            binding.tvAchievementsOfUser.visibility = View.VISIBLE
            binding.tvAchievementsOfUser.text = getString(R.string.achievements_of_user, userName)
        }
        if (isBetaFlavour) {
            binding.progressBar.visibility = View.GONE
            binding.imagesUsedByWikiText.setText(R.string.no_image)
            binding.imagesRevertedText.setText(R.string.no_image_reverted)
            binding.imagesUploadTextParam.setText(R.string.no_image_uploaded)
            binding.wikidataEdits.text = "0"
            binding.imageFeatured.text = "0"
            binding.qualityImages.text = "0"
            binding.achievementLevel.text = "0"
            setMenuVisibility(true)
            return binding.root
        }


        setWikidataEditCount()
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
    protected fun showInfoDialog() {
        launchAlert(
            resources.getString(R.string.Achievements),
            resources.getString(R.string.achievements_info_message)
        )
    }




    /**
     * To call the API to get results in form Single<JSONObject>
     * which then calls parseJson when results are fetched
     */

    private fun setAchievements() {
        binding.progressBar.visibility = View.VISIBLE
        if (checkAccount()) {
            try {
                compositeDisposable.add(
                    okHttpJsonApiClient
                        .getAchievements(userName ?: return)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { response ->
                                if (response != null) {
                                    setUploadCount(Achievements.from(response))
                                } else {
                                    Timber.d("Success")
                                    binding.layoutImageReverts.visibility = View.INVISIBLE
                                    binding.achievementBadgeImage.visibility = View.INVISIBLE
                                    // If the number of edits made by the user are more than 150,000
                                    // in some cases such high number of wiki edit counts cause the
                                    // achievements calculator to fail in some cases, for more details
                                    // refer Issue: #3295
                                    if (numberOfEdits <= 150_000) {
                                        showSnackBarWithRetry(false)
                                    } else {
                                        showSnackBarWithRetry(true)
                                    }
                                }
                            },
                            { throwable ->
                                Timber.e(throwable, "Fetching achievements statistics failed")
                                if (numberOfEdits <= 150_000) {
                                    showSnackBarWithRetry(false)
                                } else {
                                    showSnackBarWithRetry(true)
                                }
                            }
                        )
                )
            } catch (e: Exception) {
                Timber.d("Exception: ${e.message}")
            }
        }
    }

    /**
     * To call the API to fetch the count of wiki data edits
     * in the form of JavaRx Single object<JSONobject>
    </JSONobject> */

    private fun setWikidataEditCount() {
        if (StringUtils.isBlank(userName)) {
            return
        }
        compositeDisposable.add(
            okHttpJsonApiClient
                .getWikidataEdits(userName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ edits: Int ->
                    numberOfEdits = edits
                    binding.wikidataEdits.text = edits.toString()
                }, { e: Throwable ->
                    Timber.e("Error:$e")
                })
        )
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
            binding.progressBar.visibility = View.GONE
            showDismissibleSnackBar(
                requireView().findViewById(android.R.id.content),
                R.string.achievements_fetch_failed_ultimate_achievement, R.string.retry
            ) { setAchievements() }
        } else {
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
     * used to the count of images uploaded by user
     */

    private fun setUploadCount(achievements: Achievements) {
        if (checkAccount()) {
            compositeDisposable.add(okHttpJsonApiClient
                .getUploadCount(Objects.requireNonNull<String>(userName))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { uploadCount: Int? ->
                        setAchievementsUploadCount(
                            achievements,
                            uploadCount ?:0
                        )
                    },
                    { t: Throwable? ->
                        Timber.e(t, "Fetching upload count failed")
                        onError()
                    }
                ))
        }
    }

    /**
     * used to set achievements upload count and call hideProgressbar
     * @param uploadCount
     */
    private fun setAchievementsUploadCount(achievements: Achievements, uploadCount: Int) {
        // Create a new instance of Achievements with updated imagesUploaded
        val updatedAchievements = Achievements(
            achievements.uniqueUsedImages,
            achievements.articlesUsingImages,
            achievements.thanksReceived,
            achievements.featuredImages,
            achievements.qualityImages,
            uploadCount,  // Update imagesUploaded with new value
            achievements.revertCount
        )

        hideProgressBar(updatedAchievements)
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
            binding.imagesUploadedProgressbar.progress =
                100 * uploadCount / levelInfo.maxUploadCount
            binding.tvUploadedImages.text = uploadCount.toString() + "/" + levelInfo.maxUploadCount
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
            {},
            true
        )

//        binding.imagesUploadedProgressbar.setVisibility(View.INVISIBLE);
//        binding.imageRevertsProgressbar.setVisibility(View.INVISIBLE);
//        binding.imagesUsedByWikiProgressBar.setVisibility(View.INVISIBLE);
        binding.achievementBadgeImage.visibility = View.INVISIBLE
        binding.imagesUsedByWikiText.setText(R.string.no_image)
        binding.imagesRevertedText.setText(R.string.no_image_reverted)
        binding.imagesUploadTextParam.setText(R.string.no_image_uploaded)
        binding.achievementBadgeImage.visibility = View.INVISIBLE
    }

    /**
     * used to set the non revert image percentage
     * @param notRevertPercentage
     */
    private fun setImageRevertPercentage(notRevertPercentage: Int) {
        binding.imageRevertsProgressbar.visibility = View.VISIBLE
        binding.imageRevertsProgressbar.progress = notRevertPercentage
        val revertPercentage = notRevertPercentage.toString()
        binding.tvRevertedImages.text = "$revertPercentage%"
        binding.imagesRevertLimitText.text =
            resources.getString(R.string.achievements_revert_limit_message) + levelInfo.minNonRevertPercentage + "%"
    }

    /**
     * Used the inflate the fetched statistics of the images uploaded by user
     * and assign badge and level. Also stores the achievements level of the user in BasicKvStore to display in menu
     * @param achievements
     */
    private fun inflateAchievements(achievements: Achievements) {
//        binding.imagesUsedByWikiProgressBar.setVisibility(View.VISIBLE);
        binding.thanksReceived.text = achievements.thanksReceived.toString()
        binding.imagesUsedByWikiProgressBar.progress =
            100 * achievements.uniqueUsedImages / levelInfo.maxUniqueImages
        binding.tvWikiPb.text = (achievements.uniqueUsedImages.toString() + "/"
                + levelInfo.maxUniqueImages)
        binding.imageFeatured.text = achievements.featuredImages.toString()
        binding.qualityImages.text = achievements.qualityImages.toString()
        var levelUpInfoString = getString(R.string.level).uppercase()
        levelUpInfoString += " " + levelInfo.levelNumber
        binding.achievementLevel.text = levelUpInfoString
        binding.achievementBadgeImage.setImageDrawable(
            VectorDrawableCompat.create(
                resources, R.drawable.badge,
                ContextThemeWrapper(activity, levelInfo.levelStyle).theme
            )
        )
        binding.achievementBadgeText.text = levelInfo.levelNumber.toString()
        val store = BasicKvStore(this.context, userName)
        store.putString("userAchievementsLevel", levelInfo.levelNumber.toString())
    }


    /**
     * to hide progressbar
     */
    private fun hideProgressBar(achievements: Achievements) {
        if (binding.progressBar != null) {
            levelInfo = from(
                achievements.imagesUploaded,
                achievements.uniqueUsedImages,
                achievements.notRevertPercentage
            )
            inflateAchievements(achievements)
            setUploadProgress(achievements.imagesUploaded)
            setImageRevertPercentage(achievements.notRevertPercentage)
            binding.progressBar.visibility = View.GONE
        }
    }

    protected fun showUploadInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.images_uploaded),
            resources.getString(R.string.images_uploaded_explanation),
            IMAGES_UPLOADED_URL
        )
    }

    protected fun showRevertedInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.image_reverts),
            resources.getString(R.string.images_reverted_explanation),
            IMAGES_REVERT_URL
        )
    }

    protected fun showUsedByWikiInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.images_used_by_wiki),
            resources.getString(R.string.images_used_explanation),
            IMAGES_USED_URL
        )
    }

    protected fun showImagesViaNearbyInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.statistics_wikidata_edits),
            resources.getString(R.string.images_via_nearby_explanation),
            IMAGES_NEARBY_PLACES_URL
        )
    }

    protected fun showFeaturedImagesInfo() {
        launchAlertWithHelpLink(
            resources.getString(R.string.statistics_featured),
            resources.getString(R.string.images_featured_explanation),
            IMAGES_FEATURED_URL
        )
    }

    protected fun showThanksReceivedInfo() {
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
            {},
            true
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
            null,
            true
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
        private const val BADGE_IMAGE_WIDTH_RATIO = 0.4
        private const val BADGE_IMAGE_HEIGHT_RATIO = 0.3

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