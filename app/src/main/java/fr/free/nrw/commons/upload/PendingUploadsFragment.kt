package fr.free.nrw.commons.upload

import android.Manifest.permission
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_IN_PROGRESS
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_PAUSED
import fr.free.nrw.commons.contributions.Contribution.Companion.STATE_QUEUED
import fr.free.nrw.commons.contributions.ContributionController
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
    lateinit var pendingUploadsPresenter: PendingUploadsPresenter
    private lateinit var binding: FragmentPendingUploadsBinding
    private lateinit var uploadProgressActivity: UploadProgressActivity
    private lateinit var adapter: PendingUploadsAdapter
    private var contributionsSize = 0
    private var contributionsList = mutableListOf<Contribution>()

    @JvmField
    @Inject
    var controller: ContributionController? = null

    private var fab_close: Animation? = null
    private var fab_open: Animation? = null
    private var rotate_forward: Animation? = null
    private var rotate_backward: Animation? = null
    private var isFabOpen = false

    private lateinit var inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>

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


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UploadProgressActivity) {
            uploadProgressActivity = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreate(savedInstanceState)
        inAppCameraLocationPermissionLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { result ->
                val areAllGranted = result.values.all { it }

                if (areAllGranted) {
                    controller?.locationPermissionCallback?.onLocationPermissionGranted()
                } else {
                    activity?.let { currentActivity ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            currentActivity.shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)
                        ) {
                            controller?.handleShowRationaleFlowCameraLocation(
                                currentActivity,
                                inAppCameraLocationPermissionLauncher,
                                cameraPickLauncherForResult
                            )
                        } else {
                            controller?.locationPermissionCallback?.onLocationPermissionDenied(
                                currentActivity.getString(R.string.in_app_camera_location_permission_denied)
                            )
                        }
                    }
                }
            }

        binding = FragmentPendingUploadsBinding.inflate(inflater, container, false)
        binding.fabCustomGallery.setOnClickListener { launchCustomSelector() }
        binding.fabCustomGallery.setOnLongClickListener {
            showShortToast(context, R.string.custom_selector_title)
            true
        }
        pendingUploadsPresenter.onAttachView(this)
        initAdapter()
        return binding.root
    }

    private fun initAdapter() {
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
                binding.noPendingTextView.visibility = View.VISIBLE
                binding.pendingUploadsLl.visibility = View.GONE
                uploadProgressActivity.hidePendingIcons()
            } else {
                binding.noPendingTextView.visibility = View.GONE
                binding.pendingUploadsLl.visibility = View.VISIBLE
                adapter.submitList(list)
                binding.progressTextView.setText("$contributionsSize uploads left")
                if ((pausedOrQueuedUploads == contributionsSize) || CommonsApplication.isPaused) {
                    uploadProgressActivity.setPausedIcon(true)
                } else {
                    uploadProgressActivity.setPausedIcon(false)
                }
            }
        }
    }

    private fun initializeAnimations() {
        fab_open = AnimationUtils.loadAnimation(activity, R.anim.fab_open)
        fab_close = AnimationUtils.loadAnimation(activity, R.anim.fab_close)
        rotate_forward = AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        rotate_backward = AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)
    }
    private fun setListeners() {
        binding.fabPlus.setOnClickListener { animateFAB(isFabOpen) }
        binding.fabCamera.setOnClickListener {
            controller!!.initiateCameraPick(
                requireActivity(),
                inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult
            )
            animateFAB(isFabOpen)
        }
        binding.fabCamera.setOnLongClickListener {
            showShortToast(
                context,
                R.string.add_contribution_from_camera
            )
            true
        }
        binding.fabGallery.setOnClickListener {
            controller!!.initiateGalleryPick(requireActivity(), galleryPickLauncherForResult, true)
            animateFAB(isFabOpen)
        }
        binding.fabGallery.setOnLongClickListener {
            showShortToast(context, R.string.menu_from_gallery)
            true
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
    protected fun launchCustomSelector() {
        controller!!.initiateCustomGalleryPickWithPermission(
            requireActivity(),
            customSelectorLauncherForResult
        )
        animateFAB(isFabOpen)
    }

    private fun animateFAB(isFabOpen: Boolean) {
        this.isFabOpen = !isFabOpen
        if (binding.fabPlus.isShown) {
            if (isFabOpen) {
                binding.fabPlus.startAnimation(rotate_backward)
                binding.fabCamera.startAnimation(fab_close)
                binding.fabGallery.startAnimation(fab_close)
                binding.fabCustomGallery.startAnimation(fab_close)
                binding.fabCamera.hide()
                binding.fabGallery.hide()
                binding.fabCustomGallery.hide()
            } else {
                binding.fabPlus.startAnimation(rotate_forward)
                binding.fabCamera.startAnimation(fab_open)
                binding.fabGallery.startAnimation(fab_open)
                binding.fabCustomGallery.startAnimation(fab_open)
                binding.fabCamera.show()
                binding.fabGallery.show()
                binding.fabCustomGallery.show()
            }
            this.isFabOpen = !isFabOpen
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
                showShortToast(context, R.string.cancelling_upload)
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
