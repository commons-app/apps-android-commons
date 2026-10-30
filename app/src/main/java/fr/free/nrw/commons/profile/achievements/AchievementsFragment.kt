package fr.free.nrw.commons.profile.achievements

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentAchievementsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.utils.ViewUtil.showDismissibleSnackBar
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.lang3.StringUtils
import timber.log.Timber
import java.util.Objects
import javax.inject.Inject

class AchievementsFragment : CommonsDaggerSupportFragment() {
    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    // The nullable binding is cleared in onDestroyView; `binding` is the non-null accessor for this class only.
    @Suppress("ktlint:standard:backing-property-naming")
    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    // To keep track of the number of wiki edits made by a user
    private var numberOfEdits: Int = 0

    private var userName: String? = null

    private val infoDialogs = AchievementsInfoDialogs(this)

    // Built on each use so that it always fills the current views.
    private val statsBinder
        get() = AchievementsStatsBinder(this, binding, sessionManager, userName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userName = it.getString(ProfileActivity.KEY_USERNAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)

        binding.achievementInfo.setOnClickListener { infoDialogs.showInfoDialog() }
        binding.imagesUploadInfoIcon.setOnClickListener { infoDialogs.showUploadInfo() }
        binding.imagesRevertedInfoIcon.setOnClickListener { infoDialogs.showRevertedInfo() }
        binding.imagesUsedByWikiInfoIcon.setOnClickListener { infoDialogs.showUsedByWikiInfo() }
        binding.wikidataEditsIcon.setOnClickListener { infoDialogs.showImagesViaNearbyInfo() }
        binding.featuredImageIcon.setOnClickListener { infoDialogs.showFeaturedImagesInfo() }
        binding.thanksImageIcon.setOnClickListener { infoDialogs.showThanksReceivedInfo() }
        binding.qualityImageIcon.setOnClickListener { infoDialogs.showQualityImagesInfo() }

        // DisplayMetrics used to fetch the size of the screen
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        // Used for the setting the size of imageView at runtime
        // TODO REMOVE
        val params = binding.achievementBadgeImage.layoutParams as ConstraintLayout.LayoutParams
        params.height = (height * BADGE_IMAGE_HEIGHT_RATIO).toInt()
        params.width = (width * BADGE_IMAGE_WIDTH_RATIO).toInt()
        binding.achievementBadgeImage.requestLayout()
        binding.progressBar.visibility = View.VISIBLE

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
                                    // TODO Create a Method to Hide all the Statistics
//                                    binding.layoutImageReverts.visibility = View.INVISIBLE
//                                    binding.achievementBadgeImage.visibility = View.INVISIBLE
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
                            },
                        ),
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
                    statsBinder.showBadgesWithCount(view = binding.wikidataEditsIcon, count = edits)
                }, { e: Throwable ->
                    Timber.e("Error:$e")
                }),
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
            if (view == null) {
                return
            } else {
                binding.progressBar.visibility = View.GONE
                showDismissibleSnackBar(
                    requireView().findViewById(android.R.id.content),
                    R.string.achievements_fetch_failed_ultimate_achievement,
                    R.string.retry,
                ) { setAchievements() }
            }
        } else {
            if (view == null) {
                return
            }
            binding.progressBar.visibility = View.GONE
            showDismissibleSnackBar(
                requireView().findViewById(android.R.id.content),
                R.string.achievements_fetch_failed,
                R.string.retry,
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
            compositeDisposable.add(
                okHttpJsonApiClient
                    .getUploadCount(Objects.requireNonNull<String>(userName))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { uploadCount: Int? ->
                            statsBinder.bind(
                                achievements.copy(imagesUploaded = uploadCount ?: 0),
                            )
                        },
                        { t: Throwable? ->
                            Timber.e(t, "Fetching upload count failed")
                            onError()
                        },
                    ),
            )
        }
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private fun checkAccount(): Boolean {
        val currentAccount = sessionManager.currentAccount
        // Allow viewing another user's public profile without requiring login
        if (!userName.isNullOrBlank() &&
            userName != sessionManager.userName
        ) {
            return true
        }
        if (currentAccount == null) {
            Timber.d("Current account is null")
            showLongToast(requireActivity(), resources.getString(R.string.user_not_logged_in))
            sessionManager.forceLogin(activity)
            return false
        }
        return true
    }

    companion object {
        private const val BADGE_IMAGE_WIDTH_RATIO = 0.4
        private const val BADGE_IMAGE_HEIGHT_RATIO = 0.3
    }
}
