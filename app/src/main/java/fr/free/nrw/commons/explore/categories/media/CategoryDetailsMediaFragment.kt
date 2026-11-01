package fr.free.nrw.commons.explore.categories.media

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Intent
import fr.free.nrw.commons.auth.SessionManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.R
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.filepicker.FilePicker
import fr.free.nrw.commons.utils.ViewUtil.showShortToast
import javax.inject.Inject

/**
 * Media tab shown inside a category's details screen ([CategoryDetailsActivity]).
 *
 * Extends [CategoriesMediaFragment] with a floating action button that lets logged-in
 * users upload media (via camera, gallery, or the custom selector) with the category pre-filled.
 *
 * This is a separate subclass because [CategoriesMediaFragment] is also used by the
 * Featured and Uploaded-via-mobile tabs in Explore, which should not show the upload FAB.
 */
class CategoryDetailsMediaFragment : CategoriesMediaFragment() {
    @JvmField
    @Inject
    var controller: ContributionController? = null

    @Inject
    lateinit var sessionManager: SessionManager

    private var fabClose: Animation? = null
    private var fabOpen: Animation? = null
    private var rotateForward: Animation? = null
    private var rotateBackward: Animation? = null
    private var isFabOpen = false

    private lateinit var inAppCameraLocationPermissionLauncher:
        ActivityResultLauncher<Array<String>>

    private val galleryPickLauncherForResult =
        registerForActivityResult<Intent, ActivityResult>(
            StartActivityForResult(),
        ) { result: ActivityResult? ->
            controller!!.handleActivityResultWithCallback(
                requireActivity(),
            ) { callbacks: FilePicker.Callbacks? ->
                controller!!.onPictureReturnedFromGallery(
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
            controller!!.handleActivityResultWithCallback(
                requireActivity(),
            ) { callbacks: FilePicker.Callbacks? ->
                controller!!.onPictureReturnedFromCustomSelector(
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
            controller!!.handleActivityResultWithCallback(
                requireActivity(),
            ) { callbacks: FilePicker.Callbacks? ->
                controller!!.onPictureReturnedFromCamera(
                    result!!,
                    requireActivity(),
                    callbacks!!,
                )
            }
        }

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inAppCameraLocationPermissionLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { result ->
                val areAllGranted = result.values.all { it }

                if (areAllGranted) {
                    controller?.locationPermissionCallback?.onLocationPermissionGranted()
                } else {
                    activity?.let { currentActivity ->
                        if (currentActivity.shouldShowRequestPermissionRationale(
                                permission.ACCESS_FINE_LOCATION,
                            )
                        ) {
                            controller?.handleShowRationaleFlowCameraLocation(
                                currentActivity,
                                inAppCameraLocationPermissionLauncher, // Pass launcher
                                cameraPickLauncherForResult,
                            )
                        } else {
                            controller?.locationPermissionCallback?.onLocationPermissionDenied(
                                currentActivity.getString(
                                    R.string.in_app_camera_location_permission_denied,
                                ),
                            )
                        }
                    }
                }
            }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        ) {
        super.onViewCreated(view, savedInstanceState)
        initializeAnimations()
        setListeners()

        binding.paginatedSearchResultsList.addOnItemTouchListener(
            object : RecyclerView.OnItemTouchListener {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    if (e.action == MotionEvent.ACTION_DOWN && isFabOpen) {
                        animateFAB(true)
                    }
                    return false
                }
                override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
                override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
            },
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // check orientation
        binding.fabLayout.orientation =
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.HORIZONTAL
            } else {
                LinearLayout.VERTICAL
            }
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
            controller!!.setUploadCategory(requireArguments().getString("categoryName"))
            controller!!.initiateCameraPick(
                requireActivity(),
                inAppCameraLocationPermissionLauncher,
                cameraPickLauncherForResult,
            )
            animateFAB(isFabOpen)
        }
        binding.fabCamera.setOnLongClickListener {  view: View? ->
            showShortToast(
                context,
                R.string.add_contribution_from_camera,
            )
            true
        }
        binding.fabGallery.setOnClickListener { view: View? ->
            controller!!.setUploadCategory(requireArguments().getString("categoryName"))
            controller!!.initiateGalleryPick(requireActivity(), galleryPickLauncherForResult, true)
            animateFAB(isFabOpen)
        }
        binding.fabGallery.setOnLongClickListener { view: View? ->
            showShortToast(context, R.string.menu_from_gallery)
            true
        }
        binding.fabCustomGallery.setOnClickListener { launchCustomSelector() }
        binding.fabCustomGallery.setOnLongClickListener {
            showShortToast(context, R.string.custom_selector_title)
            true
        }
    }

    /**
     * Launch Custom Selector.
     */
    private fun launchCustomSelector() {
        controller!!.setUploadCategory(requireArguments().getString("categoryName"))
        controller!!.initiateCustomGalleryPickWithPermission(
            requireActivity(),
            customSelectorLauncherForResult,
        )
        animateFAB(isFabOpen)
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
     * Hides the upload FAB, closing its expanded menu first if it is open.
     */
    private fun hideContributionFab(){
        if (isFabOpen) animateFAB(true)
        binding.fabLayout.visibility = GONE

    }

    /**
     * Shows the upload FAB once the category's media has loaded.
     * Does nothing if the user is not logged in.
     */
    private fun showContributionFab(){
        if (!sessionManager.isUserLoggedIn) return
        binding.fabLayout.orientation =
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.HORIZONTAL
            } else {
                LinearLayout.VERTICAL
            }
        binding.fabLayout.visibility = VISIBLE
    }

    override fun hideInitialLoadProgress() {
        super.hideInitialLoadProgress()
        showContributionFab()
    }

    override fun showInitialLoadInProgress() {
        super.showInitialLoadInProgress()
        hideContributionFab()
    }
}
