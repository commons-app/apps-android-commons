package fr.free.nrw.commons.contributions

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.recyclerview.widget.GridLayoutManager
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentContributionsListBinding
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.wikidata.model.WikiSite
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by root on 01.06.2018.
 */
class ContributionsListFragment : ContributionsListCallbacksBase() {
    @JvmField
    @Inject
    var cameraCoordinator: ContributionCameraCoordinator? = null

    @JvmField
    @Inject
    var imagePicker: ContributionImagePicker? = null

    @JvmField
    @Inject
    var uploadCoordinator: ContributionUploadCoordinator? = null

    @JvmField
    @Inject
    var mediaClient: MediaClient? = null

    @JvmField
    @Inject
    var mediaDataExtractor: MediaDataExtractor? = null

    @JvmField
    @Named(NetworkingModule.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    @Inject
    var languageWikipediaSite: WikiSite? = null

    @JvmField
    @Inject
    var contributionsListPresenter: ContributionsListPresenter? = null

    @JvmField
    @Inject
    var sessionManager: SessionManager? = null

    lateinit var fabHelper: ContributionsListFabHelper
    override lateinit var wikipediaHelper: ContributionsListWikipediaHelper

    private lateinit var inAppCameraLocationPermissionLauncher:
        ActivityResultLauncher<Array<String>>

    private var userName: String? = null

    private val galleryPickLauncherForResult =
        registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult(),
        ) { result: ActivityResult? ->
            uploadCoordinator!!.handleActivityResultWithCallback(
                requireActivity(),
            ) { callbacks: FilePicker.Callbacks? ->
                imagePicker!!.onPictureReturnedFromGallery(
                    result!!,
                    requireActivity(),
                    callbacks!!,
                )
            }
        }

    private val customSelectorLauncherForResult =
        registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult(),
        ) { result: ActivityResult? ->
            uploadCoordinator!!.handleActivityResultWithCallback(
                requireActivity(),
            ) { callbacks: FilePicker.Callbacks? ->
                FilePicker.onPictureReturnedFromCustomSelector(
                    result!!,
                    requireActivity(),
                    callbacks!!,
                )
            }
        }

    private val cameraPickLauncherForResult =
        registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult(),
        ) { result: ActivityResult? ->
            uploadCoordinator!!.handleActivityResultWithCallback(
                requireActivity(),
            ) { callbacks: FilePicker.Callbacks? ->
                FilePicker.onPictureReturnedFromCamera(
                    result!!,
                    requireActivity(),
                    callbacks!!,
                )
            }
        }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Now that we are allowing this fragment to be started for
        // any userName- we expect it to be passed as an argument
        if (arguments != null) {
            userName = requireArguments().getString(ProfileActivity.KEY_USERNAME)
        }

        if (userName.isNullOrEmpty()) {
            userName = sessionManager!!.userName
        }
        inAppCameraLocationPermissionLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { result ->
                handleCameraLocationPermissionResult(result)
            }

        wikipediaHelper =
            ContributionsListWikipediaHelper(
                fragment = this,
                languageWikipediaSiteProvider = { languageWikipediaSite },
            )
        val fabLaunchers =
            ContributionsListFabHelper.Launchers(
                inAppCameraLocationPermissionLauncher = inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult = cameraPickLauncherForResult,
                galleryPickLauncherForResult = galleryPickLauncherForResult,
                customSelectorLauncherForResult = customSelectorLauncherForResult,
            )
        fabHelper =
            ContributionsListFabHelper(
                fragment = this,
                bindingProvider = { binding },
                controllerProvider = { controller },
                launchers = fabLaunchers,
            )
    }

    /**
     * Forwards camera location permission results while this fragment is attached.
     *
     * @param results outcomes from the location permission launcher
     */
    internal fun handleCameraLocationPermissionResult(results: Map<String, Boolean>) {
        activity?.let { currentActivity ->
            cameraCoordinator?.handleLocationPermissionResult(
                currentActivity,
                results,
                inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult,
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding =
            FragmentContributionsListBinding.inflate(
                inflater,
                container,
                false,
            )
        rvContributionsList = binding!!.contributionsList

        contributionsListPresenter!!.onAttachView(this)

        if (sessionManager!!.userName == userName) {
            binding!!.tvContributionsOfUser.visibility = View.GONE
            binding!!.fabLayout.visibility = View.VISIBLE
            // Set correct FAB orientation based on current orientation
            fabHelper.syncFabOrientation(resources.configuration.orientation)
        } else {
            binding!!.tvContributionsOfUser.visibility = View.VISIBLE
            binding!!.tvContributionsOfUser.text =
                getString(R.string.contributions_of_user, userName)
            binding!!.fabLayout.visibility = View.GONE
        }

        adapter =
            ContributionsListRecyclerViewHelper.createAdapter(
                this,
                mediaClient!!,
                mediaDataExtractor!!,
                compositeDisposable,
            )

        // pull down to refresh only enabled for self user.
        if (sessionManager!!.userName == userName) {
            binding!!.swipeRefreshLayout.setOnRefreshListener {
                contributionsListPresenter!!.refreshList(
                    binding!!.swipeRefreshLayout,
                )
            }
        } else {
            binding!!.swipeRefreshLayout.isEnabled = false
        }

        return binding!!.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        ContributionsListRecyclerViewHelper.initializeRecyclerView(this, userName)
        fabHelper.initializeAnimations()
        fabHelper.setListeners()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // check orientation
        fabHelper.syncFabOrientation(newConfig.orientation)
        rvContributionsList
            ?.setLayoutManager(
                GridLayoutManager(
                    context,
                    ContributionsListRecyclerViewHelper.spanCountForOrientation(newConfig.orientation),
                ),
            )
    }

    fun scrollToTop() {
        rvContributionsList!!.smoothScrollToPosition(0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val layoutManager = rvContributionsList?.layoutManager as GridLayoutManager?
        outState.putParcelable(RV_STATE, layoutManager!!.onSaveInstanceState())
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (null != savedInstanceState) {
            val savedRecyclerLayoutState =
                BundleCompat.getParcelable(savedInstanceState, RV_STATE, Parcelable::class.java)
            rvContributionsList!!.layoutManager!!.onRestoreInstanceState(savedRecyclerLayoutState)
        }
    }

    interface Callback {
        fun notifyDataSetChanged()

        fun showDetail(
            position: Int,
            isWikipediaButtonDisplayed: Boolean,
        )

        // Notify the viewpager that number of items have changed.
        fun viewPagerNotifyDataSetChanged()
    }
}
