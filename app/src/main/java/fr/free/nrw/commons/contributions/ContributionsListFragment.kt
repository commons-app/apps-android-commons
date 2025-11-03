package fr.free.nrw.commons.contributions

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.paging.PagedList
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import androidx.recyclerview.widget.SimpleItemAnimator
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.MediaDataExtractor
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.contributions.WikipediaInstructionsDialogFragment.Companion.newInstance
import fr.free.nrw.commons.databinding.FragmentContributionsListBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.di.NetworkingModule
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import fr.free.nrw.commons.utils.copyToClipboard
import fr.free.nrw.commons.utils.handleWebUrl
import fr.free.nrw.commons.wikidata.model.WikiSite
import javax.inject.Inject
import javax.inject.Named


/**
 * Created by root on 01.06.2018.
 */
class ContributionsListFragment : CommonsDaggerSupportFragment(), ContributionsListContract.View,
    ContributionsListAdapter.Callback, WikipediaInstructionsDialogFragment.Callback {
    @JvmField
    @Inject
    var controller: ContributionController? = null

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

    private var binding: FragmentContributionsListBinding? = null
    private var fabClose: Animation? = null
    private var fabOpen: Animation? = null
    private var rotateForward: Animation? = null
    private var rotateBackward: Animation? = null
    private var isFabOpen = false

    private lateinit var inAppCameraLocationPermissionLauncher:
            ActivityResultLauncher<Array<String>>

    @VisibleForTesting
    var rvContributionsList: RecyclerView? = null

    @VisibleForTesting
    var adapter: ContributionsListAdapter? = null

    @VisibleForTesting
    var callback: Callback? = null

    private val spanCountLandscape = 3
    private val spanCountPortrait = 1

    private var contributionsSize = 0
    private var userName: String? = null

    private val galleryPickLauncherForResult = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        controller!!.handleActivityResultWithCallback(requireActivity()
        ) { callbacks: FilePicker.Callbacks? ->
            controller!!.onPictureReturnedFromGallery(
                result!!, requireActivity(), callbacks!!
            )
        }
    }

    private val customSelectorLauncherForResult = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        controller!!.handleActivityResultWithCallback(requireActivity()
        ) { callbacks: FilePicker.Callbacks? ->
            controller!!.onPictureReturnedFromCustomSelector(
                result!!, requireActivity(), callbacks!!
            )
        }
    }

    private val cameraPickLauncherForResult = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        controller!!.handleActivityResultWithCallback(requireActivity()
        ) { callbacks: FilePicker.Callbacks? ->
            controller!!.onPictureReturnedFromCamera(
                result!!, requireActivity(), callbacks!!
            )
        }
    }

    @SuppressLint("NewApi")
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        //Now that we are allowing this fragment to be started for
        // any userName- we expect it to be passed as an argument
        if (arguments != null) {
            userName = requireArguments().getString(ProfileActivity.KEY_USERNAME)
        }

        if (userName.isNullOrEmpty()) {
            userName = sessionManager!!.userName
        }
        inAppCameraLocationPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { result ->
            val areAllGranted = result.values.all { it }

            if (areAllGranted) {
                controller?.locationPermissionCallback?.onLocationPermissionGranted()
            } else {
                activity?.let { currentActivity ->
                    if (currentActivity.shouldShowRequestPermissionRationale(
                            permission.ACCESS_FINE_LOCATION)) {
                        controller?.handleShowRationaleFlowCameraLocation(
                            currentActivity,
                            inAppCameraLocationPermissionLauncher, // Pass launcher
                            cameraPickLauncherForResult
                        )
                    } else {
                        controller?.locationPermissionCallback?.onLocationPermissionDenied(
                            currentActivity.getString(
                                R.string.in_app_camera_location_permission_denied)
                        )
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContributionsListBinding.inflate(
            inflater, container, false
        )
        rvContributionsList = binding!!.contributionsList

        contributionsListPresenter!!.onAttachView(this)
        binding!!.fabCustomGallery.setOnClickListener { v: View? -> launchCustomSelector() }
        binding!!.fabCustomGallery.setOnLongClickListener { view: View? ->
            showShortToast(context, R.string.custom_selector_title)
            true
        }

        if (sessionManager!!.userName == userName) {
            binding!!.tvContributionsOfUser.visibility = View.GONE
            binding!!.fabLayout.visibility = View.VISIBLE
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
                    binding!!.swipeRefreshLayout
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null && parentFragment is ContributionsFragment) {
            callback = (parentFragment as ContributionsFragment)
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null //To avoid possible memory leak
    }

    private fun initAdapter() {
        adapter = ContributionsListAdapter(this,
            mediaClient!!,
            mediaDataExtractor!!,
            compositeDisposable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initializeAnimations()
        setListeners()
    }

    private fun initRecyclerView() {
        val layoutManager = GridLayoutManager(
            context,
            getSpanCount(resources.configuration.orientation)
        )
        rvContributionsList!!.layoutManager = layoutManager

        //Setting flicker animation of recycler view to false.
        val animator = rvContributionsList!!.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }

        contributionsListPresenter!!.setup(
            userName,
            sessionManager!!.userName == userName
        )
        contributionsListPresenter!!.contributionList?.observe(
            viewLifecycleOwner
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
        adapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                contributionsSize = adapter!!.itemCount
                if (callback != null) {
                    callback!!.notifyDataSetChanged()
                }
                if (itemCount > 0 && positionStart == 0) {
                    if (adapter!!.getContributionForPosition(positionStart) != null) {
                        rvContributionsList!!
                            .scrollToPosition(0) //Newly upload items are always added to the top
                    }
                }
            }

            /**
             * Called whenever items in the list have changed
             * Calls viewPagerNotifyDataSetChanged() that will notify the viewpager
             */
            override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
                super.onItemRangeChanged(positionStart, itemCount)
                if (callback != null) {
                    callback!!.viewPagerNotifyDataSetChanged()
                }
            }
        })

        //Fab close on touch outside (Scrolling or taping on item triggers this action).
        rvContributionsList!!.addOnItemTouchListener(object : OnItemTouchListener {
            /**
             * Silently observe and/or take over touch events sent to the RecyclerView before
             * they are handled by either the RecyclerView itself or its child views.
             */
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_DOWN) {
                    if (isFabOpen) {
                        animateFAB(true)
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
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                //required abstract method DO NOT DELETE
            }

            /**
             * Called when a child of RecyclerView does not want RecyclerView and its ancestors
             * to intercept touch events with [ViewGroup.onInterceptTouchEvent].
             *
             * @param disallowIntercept True if the child does not want the parent to intercept
             * touch events.
             */
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                //required abstract method DO NOT DELETE
            }
        })
    }

    private fun getSpanCount(orientation: Int): Int {
        return if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            spanCountLandscape
        else
            spanCountPortrait
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // check orientation
        binding!!.fabLayout.orientation =
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
                LinearLayout.HORIZONTAL
            else
                LinearLayout.VERTICAL
        rvContributionsList
            ?.setLayoutManager(
                GridLayoutManager(context, getSpanCount(newConfig.orientation))
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
            controller!!.initiateCameraPick(
                requireActivity(),
                inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult
            )
            animateFAB(isFabOpen)
        }
        binding!!.fabCamera.setOnLongClickListener { view: View? ->
            showShortToast(
                context,
                R.string.add_contribution_from_camera
            )
            true
        }
        binding!!.fabGallery.setOnClickListener { view: View? ->
            controller!!.initiateGalleryPick(requireActivity(), galleryPickLauncherForResult, true)
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
        controller!!.initiateCustomGalleryPickWithPermission(
            requireActivity(),
            customSelectorLauncherForResult
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

    override fun openMediaDetail(contribution: Int, isWikipediaPageExists: Boolean) {
        if (null != callback) { //Just being safe, ideally they won't be called when detached
            callback!!.showDetail(contribution, isWikipediaPageExists)
        }
    }

    /**
     * Handle callback for wikipedia icon clicked
     *
     * @param contribution
     */
    override fun addImageToWikipedia(contribution: Contribution?) {
        showAlertDialog(
            requireActivity(),
            getString(R.string.add_picture_to_wikipedia_article_title),
            getString(R.string.add_picture_to_wikipedia_article_desc),
            {
                if (contribution != null) {
                    showAddImageToWikipediaInstructions(contribution)
                }
            }, {})
    }

    /**
     * Display confirmation dialog with instructions when the user tries to add image to wikipedia
     *
     * @param contribution
     */
    private fun showAddImageToWikipediaInstructions(contribution: Contribution) {
        val fragmentManager = this.parentFragmentManager
        val fragment = newInstance(contribution)
        fragment.callback =
            WikipediaInstructionsDialogFragment.Callback {
                contribution: Contribution?,
                copyWikicode: Boolean ->
                onConfirmClicked(
                    contribution,
                    copyWikicode
                )
            }
        fragment.show(fragmentManager, "WikimediaFragment")
    }


    fun getMediaAtPosition(i: Int): Media? {
        if (adapter!!.getContributionForPosition(i) != null) {
            return adapter!!.getContributionForPosition(i)!!.media
        }
        return null
    }

    val totalMediaCount: Int
        get() = contributionsSize

    /**
     * Open the editor for the language Wikipedia
     *
     * @param contribution
     */
    override fun onConfirmClicked(contribution: Contribution?, copyWikicode: Boolean) {
        if (copyWikicode) {
            requireContext().copyToClipboard("wikicode", contribution!!.media.wikiCode)
        }

        val url =
            languageWikipediaSite!!.mobileUrl() + "/wiki/" + (contribution!!.wikidataPlace
                ?.getWikipediaPageTitle())
        handleWebUrl(requireContext(), url.toUri())
    }

    fun getContributionStateAt(position: Int): Int {
        return adapter!!.getContributionForPosition(position)!!.state
    }

     interface Callback {
        fun notifyDataSetChanged()

        fun showDetail(position: Int, isWikipediaButtonDisplayed: Boolean)

        // Notify the viewpager that number of items have changed.
        fun viewPagerNotifyDataSetChanged()
    }

    companion object {
        private const val RV_STATE = "rv_scroll_state"
    }
}
