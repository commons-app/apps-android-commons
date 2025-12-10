package fr.free.nrw.commons.upload

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.paging.PagedList
import android.Manifest.permission
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_IN_PROGRESS
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_PAUSED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_QUEUED
import fr.free.nrw.commons.databinding.FragmentPendingUploadsBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.utils.DialogUtil.showAlertDialog
import fr.free.nrw.commons.utils.ViewUtil
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import java.util.Locale
import javax.inject.Inject

/**
 * Fragment for showing pending uploads in Upload Progress Activity. This fragment provides
 * functionality for the user to pause uploads.
 */
class PendingUploadsFragment :
    CommonsDaggerSupportFragment(),
    PendingUploadsContract.View,
    PendingUploadsAdapter.Callback {
    @Inject
    lateinit var contributionController: ContributionController

    @Inject
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter

    private lateinit var binding: FragmentPendingUploadsBinding

    private lateinit var uploadProgressActivity: UploadProgressActivity

    private lateinit var adapter: PendingUploadsAdapter

    private var contributionsSize = 0

    private var contributionsList = mutableListOf<Contribution>()

    private var fabClose: Animation? = null
    private var fabOpen: Animation? = null
    private var rotateForward: Animation? = null
    private var rotateBackward: Animation? = null
    private var isFabOpen = false
    private val customSelectorLauncherForResult = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        contributionController.handleActivityResultWithCallback(requireActivity()
        ) { callbacks: FilePicker.Callbacks? ->
            contributionController.onPictureReturnedFromCustomSelector(
                result!!, requireActivity(), callbacks!!
            )
        }
    }

    private val galleryPickLauncherForResult = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        contributionController.handleActivityResultWithCallback(requireActivity()
        ) { callbacks: FilePicker.Callbacks? ->
            contributionController.onPictureReturnedFromGallery(
                result!!, requireActivity(), callbacks!!
            )
        }
    }

    private val cameraPickLauncherForResult = registerForActivityResult<Intent, ActivityResult>(
        StartActivityForResult()
    ) { result: ActivityResult? ->
        contributionController.handleActivityResultWithCallback(requireActivity()
        ) { callbacks: FilePicker.Callbacks? ->
            contributionController.onPictureReturnedFromCamera(
                result!!, requireActivity(), callbacks!!
            )
        }
    }

    private lateinit var inAppCameraLocationPermissionLauncher:
            ActivityResultLauncher<Array<String>>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UploadProgressActivity) {
            uploadProgressActivity = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //initialise the location permission launcher
        inAppCameraLocationPermissionLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { result ->
                val areAllGranted = result.values.all { it }

                if (areAllGranted) {
                    contributionController.locationPermissionCallback?.onLocationPermissionGranted()
                } else {
                    activity?.let { currentActivity ->
                        if (currentActivity.shouldShowRequestPermissionRationale(
                                permission.ACCESS_FINE_LOCATION)) {
                        }
                        contributionController.locationPermissionCallback?.onLocationPermissionDenied(
                            currentActivity.getString(
                                R.string.in_app_camera_location_permission_denied)
                        )
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreate(savedInstanceState)
        binding = FragmentPendingUploadsBinding.inflate(inflater, container, false)
        pendingUploadsPresenter.onAttachView(this)
        initAdapter()
        return binding.root
    }

    fun initAdapter() {
        adapter = PendingUploadsAdapter(this)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        initializeAnimations()
        setListeners()
    }

    private fun initializeAnimations() {
        fabOpen = AnimationUtils.loadAnimation(activity, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(activity, R.anim.fab_close)
        rotateForward = AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        rotateBackward = AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)
    }

    private fun setListeners() {
        binding.fabPlus.setOnClickListener { view: View? -> animateFAB(isFabOpen) }

        binding.fabCamera.setOnClickListener { view: View? ->
            contributionController.initiateCameraPick(
                requireActivity(),
                inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult
            )
            animateFAB(isFabOpen)
        }
        binding.fabCamera.setOnLongClickListener { view: View? ->
            showShortToast(
                context,
                R.string.add_contribution_from_camera
            )
            true
        }

        binding.fabGallery.setOnClickListener { view: View? ->
            contributionController.initiateGalleryPick(requireActivity(), galleryPickLauncherForResult, true)
            animateFAB(isFabOpen)
        }
        binding.fabGallery.setOnLongClickListener { view: View? ->
            showShortToast(context, R.string.menu_from_gallery)
            true
        }

        binding.fabCustomGallery.setOnClickListener { v: View? ->
            launchCustomSelector()
            animateFAB(isFabOpen)
        }
        binding.fabCustomGallery.setOnLongClickListener { view: View? ->
            showShortToast(context, R.string.custom_selector_title)
            true
        }
    }

    /**
     * launch Custom Selector.
     */
    private fun launchCustomSelector() {
        contributionController.initiateCustomGalleryPickWithPermission(
            requireActivity(),
            customSelectorLauncherForResult
        )
    }

    private fun animateFAB(isFabOpen: Boolean) {
        this.isFabOpen = !isFabOpen
        if (binding.fabPlus.isShown) {
            if (isFabOpen) {
                binding.fabPlus.startAnimation(rotateBackward)
                binding.fabCamera.startAnimation(fabClose)
                binding.fabGallery.startAnimation(fabClose)
                binding.fabCustomGallery.startAnimation(fabClose)
                binding.fabCamera.hide()
                binding.fabGallery.hide()
                binding.fabCustomGallery.hide()
            } else {
                binding.fabPlus.startAnimation(rotateForward)
                binding.fabCamera.startAnimation(fabOpen)
                binding.fabGallery.startAnimation(fabOpen)
                binding.fabCustomGallery.startAnimation(fabOpen)
                binding.fabCamera.show()
                binding.fabGallery.show()
                binding.fabCustomGallery.show()
            }
            this.isFabOpen = !isFabOpen
        }
    }

    /**
     * Initializes the recycler view.
     */
    private fun initRecyclerView() {
        binding.pendingUploadsRecyclerView.setLayoutManager(LinearLayoutManager(this.context))
        binding.pendingUploadsRecyclerView.adapter = adapter
        pendingUploadsPresenter.setup()
        pendingUploadsPresenter.totalContributionList
            .observe(viewLifecycleOwner) { list: PagedList<Contribution> ->
            contributionsSize = list.size
            contributionsList = mutableListOf()
            var pausedOrQueuedUploads = 0
            list.forEach {
                if (it != null) {
                    if (it.state == STATE_PAUSED ||
                        it.state == STATE_QUEUED ||
                        it.state == STATE_IN_PROGRESS
                    ) {
                        contributionsList.add(it)
                    }
                    if (it.state == STATE_PAUSED || it.state == STATE_QUEUED) {
                        pausedOrQueuedUploads++
                    }
                }
            }
            if (contributionsSize == 0) {
                binding.nopendingTextView.visibility = View.VISIBLE
                binding.pendingUplaodsLl.visibility = View.GONE
                uploadProgressActivity.hidePendingIcons()
                binding.fabLayout.visibility = View.VISIBLE
            } else {
                binding.nopendingTextView.visibility = View.GONE
                binding.pendingUplaodsLl.visibility = View.VISIBLE
                adapter.submitList(list)
                binding.progressTextView.setText("$contributionsSize uploads left")
                if ((pausedOrQueuedUploads == contributionsSize) || CommonsApplication.isPaused) {
                    uploadProgressActivity.setPausedIcon(true)
                } else {
                    uploadProgressActivity.setPausedIcon(false)
                }
                binding.fabLayout.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Cancels a specific upload after getting a confirmation from the user using Dialog.
     */
    override fun deleteUpload(contribution: Contribution?) {
        val activity = requireActivity()
        val locale = Locale.getDefault()
        showAlertDialog(
            activity,
            String.format(locale, activity.getString(R.string.cancelling_upload)),
            String.format(locale, activity.getString(R.string.cancel_upload_dialog)),
            String.format(locale, activity.getString(R.string.yes)),
            String.format(locale, activity.getString(R.string.no)),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                pendingUploadsPresenter.deleteUpload(
                    contribution, requireContext().applicationContext,
                )
            },
            {},
        )
    }

    /**
     * Restarts all the paused uploads.
     */
    fun restartUploads() = pendingUploadsPresenter.restartUploads(
        contributionsList, 0, requireContext().applicationContext
    )

    /**
     * Pauses all the ongoing uploads.
     */
    fun pauseUploads() = pendingUploadsPresenter.pauseUploads()

    /**
     * Cancels all the uploads after getting a confirmation from the user using Dialog.
     */
    fun deleteUploads() {
        val activity = requireActivity()
        val locale = Locale.getDefault()
        showAlertDialog(
            activity,
            String.format(locale, activity.getString(R.string.cancelling_all_the_uploads)),
            String.format(locale, activity.getString(R.string.are_you_sure_that_you_want_cancel_all_the_uploads)),
            String.format(locale, activity.getString(R.string.yes)),
            String.format(locale, activity.getString(R.string.no)),
            {
                ViewUtil.showShortToast(context, R.string.cancelling_upload)
                uploadProgressActivity.hidePendingIcons()
                pendingUploadsPresenter.deleteUploads(
                    listOf(
                        STATE_QUEUED,
                        STATE_IN_PROGRESS,
                        STATE_PAUSED,
                    ),
                )
            },
            {},
        )
    }
}
