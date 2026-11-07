package fr.free.nrw.commons.contributions

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.SimpleItemAnimator
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.databinding.FragmentContributionsListBinding
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.media.DeletedFileActivity
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.wikidata.model.WikiSite
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by root on 01.06.2018.
 */
class ContributionsListFragment :
    BaseContributionsListFragment(),
    ContributionsListAdapter.Callback {
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
    lateinit var wikipediaHelper: ContributionsListWikipediaHelper

    private lateinit var inAppCameraLocationPermissionLauncher:
        ActivityResultLauncher<Array<String>>

    private val spanCountLandscape = 3
    private val spanCountPortrait = 1

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

        initAdapter()

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

    private fun initAdapter() {
        adapter =
            ContributionsListAdapter(
                this,
                mediaClient!!,
                mediaDataExtractor!!,
                compositeDisposable,
            )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        fabHelper.initializeAnimations()
        fabHelper.setListeners()
    }

    private fun initRecyclerView() {
        val layoutManager =
            GridLayoutManager(
                context,
                getSpanCount(resources.configuration.orientation),
            )
        rvContributionsList!!.layoutManager = layoutManager

        // Setting flicker animation of recycler view to false.
        val animator = rvContributionsList!!.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        contributionsListPresenter!!.setup(
            userName,
            sessionManager!!.userName == userName,
        )
        contributionsListPresenter!!.contributionList?.observe(
            viewLifecycleOwner,
        ) { list: PagedList<Contribution>? ->
            if (list != null) {
                contributionsSize = list.size
            }
            adapter!!.submitList(list)
            if (callback != null) {
                callback!!.notifyDataSetChanged()
            }
        }
        rvContributionsList!!.adapter = adapter
        adapter!!.registerAdapterDataObserver(
            object : AdapterDataObserver() {
                override fun onItemRangeInserted(
                    positionStart: Int,
                    itemCount: Int,
                ) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    contributionsSize = adapter!!.itemCount
                    if (callback != null) {
                        callback!!.notifyDataSetChanged()
                    }
                    if (itemCount > 0 && positionStart == 0) {
                        if (adapter!!.getContributionForPosition(positionStart) != null) {
                            rvContributionsList!!
                                .scrollToPosition(0) // Newly upload items are always added to the top
                        }
                    }
                }

                /**
                 * Called whenever items in the list have changed
                 * Calls viewPagerNotifyDataSetChanged() that will notify the viewpager
                 */
                override fun onItemRangeChanged(
                    positionStart: Int,
                    itemCount: Int,
                ) {
                    super.onItemRangeChanged(positionStart, itemCount)
                    if (callback != null) {
                        callback!!.viewPagerNotifyDataSetChanged()
                    }
                }
            },
        )

        // Fab close on touch outside (Scrolling or taping on item triggers this action).
        rvContributionsList!!.addOnItemTouchListener(
            object : OnItemTouchListener {
                /**
                 * Silently observe and/or take over touch events sent to the RecyclerView before
                 * they are handled by either the RecyclerView itself or its child views.
                 */
                override fun onInterceptTouchEvent(
                    rv: RecyclerView,
                    e: MotionEvent,
                ): Boolean {
                    if (e.action == MotionEvent.ACTION_DOWN) {
                        if (fabHelper.isFabOpen) {
                            fabHelper.animateFAB(true)
                        }
                    }
                    return false
                }

                /**
                 * Process a touch event as part of a gesture that was claimed by returning true
                 * from a previous call to [.onInterceptTouchEvent].
                 *
                 * @param rv
                 * @param e  MotionEvent describing the touch event. All coordinates are in the
                 * RecyclerView's coordinate system.
                 */
                override fun onTouchEvent(
                    rv: RecyclerView,
                    e: MotionEvent,
                ) {
                    // required abstract method DO NOT DELETE
                }

                /**
                 * Called when a child of RecyclerView does not want RecyclerView and its ancestors
                 * to intercept touch events with [ViewGroup.onInterceptTouchEvent].
                 *
                 * @param disallowIntercept True if the child does not want the parent to intercept
                 * touch events.
                 */
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                    // required abstract method DO NOT DELETE
                }
            },
        )
    }

    private fun getSpanCount(orientation: Int): Int =
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            spanCountLandscape
        } else {
            spanCountPortrait
        }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // check orientation
        fabHelper.syncFabOrientation(newConfig.orientation)
        rvContributionsList
            ?.setLayoutManager(
                GridLayoutManager(context, getSpanCount(newConfig.orientation)),
            )
    }

    private fun initializeAnimations() {
        fabOpen = AnimationUtils.loadAnimation(activity, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(activity, R.anim.fab_close)
        rotateForward = AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        rotateBackward = AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)
    }

    private fun setListeners() {
        binding!!.fabPlus.setOnClickListener { view: View? -> animateFAB(isFabOpen) }
        binding!!.fabCamera.setOnClickListener { view: View? ->
            cameraCoordinator!!.initiateCameraPick(
                requireActivity(),
                inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult,
            )
            animateFAB(isFabOpen)
        }
        binding!!.fabCamera.setOnLongClickListener { view: View? ->
            showShortToast(
                context,
                R.string.add_contribution_from_camera,
            )
            true
        }
        binding!!.fabGallery.setOnClickListener { view: View? ->
            imagePicker!!.initiateGalleryPick(requireActivity(), galleryPickLauncherForResult, true)
            animateFAB(isFabOpen)
        }
        binding!!.fabGallery.setOnLongClickListener { view: View? ->
            showShortToast(context, R.string.menu_from_gallery)
            true
        }
    }

    /**
     * Launch Custom Selector.
     */
    private fun launchCustomSelector() {
        imagePicker!!.initiateCustomGalleryPickWithPermission(
            requireActivity(),
            customSelectorLauncherForResult,
        )
        animateFAB(isFabOpen)
    }

    fun scrollToTop() {
        rvContributionsList!!.smoothScrollToPosition(0)
    }

    private fun animateFAB(isFabOpen: Boolean) {
        this.isFabOpen = !isFabOpen
        if (binding!!.fabPlus.isShown) {
            if (isFabOpen) {
                binding!!.fabPlus.startAnimation(rotateBackward)
                binding!!.fabCamera.startAnimation(fabClose)
                binding!!.fabGallery.startAnimation(fabClose)
                binding!!.fabCustomGallery.startAnimation(fabClose)
                binding!!.fabCamera.hide()
                binding!!.fabGallery.hide()
                binding!!.fabCustomGallery.hide()
            } else {
                binding!!.fabPlus.startAnimation(rotateForward)
                binding!!.fabCamera.startAnimation(fabOpen)
                binding!!.fabGallery.startAnimation(fabOpen)
                binding!!.fabCustomGallery.startAnimation(fabOpen)
                binding!!.fabCamera.show()
                binding!!.fabGallery.show()
                binding!!.fabCustomGallery.show()
            }
            this.isFabOpen = !isFabOpen
        }
    }

    /**
     * Shows welcome message if user has no contributions yet i.e. new user.
     */
    override fun showWelcomeTip(numberOfUploads: Boolean) {
        binding!!.noContributionsYet.visibility =
            if (numberOfUploads) View.VISIBLE else View.GONE
    }

    /**
     * Responsible to set progress bar invisible and visible
     *
     * @param shouldShow True when contributions list should be hidden.
     */
    override fun showProgress(shouldShow: Boolean) {
        binding!!.loadingContributionsProgressBar.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
    }

    override fun showNoContributionsUI(shouldShow: Boolean) {
        binding!!.noContributionsYet.visibility =
            if (shouldShow) View.VISIBLE else View.GONE
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

    override fun openMediaDetail(
        contribution: Int,
        isWikipediaPageExists: Boolean,
    ) {
        val item = adapter?.getContributionForPosition(contribution)
        val filename = item?.media?.filename
        if (item?.state == Contribution.STATE_DELETED && !filename.isNullOrEmpty()) {
            DeletedFileActivity.start(requireContext(), filename)
            return
        }
        if (null != callback) { // Just being safe, ideally they won't be called when detached
            callback!!.showDetail(contribution, isWikipediaPageExists)
        }
    }

    /**
     * Handle callback for wikipedia icon clicked
     *
     * @param contribution
     */
    override fun addImageToWikipedia(contribution: Contribution?) {
        wikipediaHelper.addImageToWikipedia(contribution)
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
