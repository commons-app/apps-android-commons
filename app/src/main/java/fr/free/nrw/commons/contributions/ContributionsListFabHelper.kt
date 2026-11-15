package fr.free.nrw.commons.contributions

import android.content.Intent
import android.content.res.Configuration
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentContributionsListBinding
import fr.free.nrw.commons.utils.ViewUtil.showShortToast

/**
 * Helper class managing floating action button (FAB) orientation, animations, and upload action triggers.
 */
class ContributionsListFabHelper(
    private val fragment: Fragment,
    private val bindingProvider: () -> FragmentContributionsListBinding?,
    private val controllerProvider: () -> ContributionController?,
    private val launchers: Launchers,
) {
    /**
     * Bundled activity result launchers required for media pick and permission actions.
     */
    data class Launchers(
        val inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>,
        val cameraPickLauncherForResult: ActivityResultLauncher<Intent>,
        val galleryPickLauncherForResult: ActivityResultLauncher<Intent>,
        val customSelectorLauncherForResult: ActivityResultLauncher<Intent>,
    )

    var fabClose: Animation? = null
    var fabOpen: Animation? = null
    var rotateForward: Animation? = null
    var rotateBackward: Animation? = null
    var isFabOpen = false

    fun initializeAnimations() {
        val activity = fragment.activity ?: return
        fabOpen = AnimationUtils.loadAnimation(activity, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(activity, R.anim.fab_close)
        rotateForward = AnimationUtils.loadAnimation(activity, R.anim.rotate_forward)
        rotateBackward = AnimationUtils.loadAnimation(activity, R.anim.rotate_backward)
    }

    fun setListeners() {
        val binding = bindingProvider() ?: return
        binding.fabPlus.setOnClickListener { animateFAB(isFabOpen) }
        binding.fabCamera.setOnClickListener {
            controllerProvider()?.initiateCameraPick(
                fragment.requireActivity(),
                launchers.inAppCameraLocationPermissionLauncher,
                launchers.cameraPickLauncherForResult,
            )
            animateFAB(isFabOpen)
        }
        binding.fabCamera.setOnLongClickListener {
            showShortToast(
                fragment.context,
                R.string.add_contribution_from_camera,
            )
            true
        }
        binding.fabGallery.setOnClickListener {
            controllerProvider()?.initiateGalleryPick(
                fragment.requireActivity(),
                launchers.galleryPickLauncherForResult,
                true,
            )
            animateFAB(isFabOpen)
        }
        binding.fabGallery.setOnLongClickListener {
            showShortToast(fragment.context, R.string.menu_from_gallery)
            true
        }
        binding.fabCustomGallery.setOnClickListener { launchCustomSelector() }
        binding.fabCustomGallery.setOnLongClickListener {
            showShortToast(fragment.context, R.string.custom_selector_title)
            true
        }
    }

    /**
     * Launch Custom Selector.
     */
    fun launchCustomSelector() {
        controllerProvider()?.initiateCustomGalleryPickWithPermission(
            fragment.requireActivity(),
            launchers.customSelectorLauncherForResult,
        )
        animateFAB(isFabOpen)
    }

    fun animateFAB(isFabOpen: Boolean) {
        val binding = bindingProvider() ?: return
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

    fun syncFabOrientation(orientation: Int) {
        val binding = bindingProvider() ?: return
        // check orientation
        binding.fabLayout.orientation =
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                LinearLayout.HORIZONTAL
            } else {
                LinearLayout.VERTICAL
            }
    }
}
