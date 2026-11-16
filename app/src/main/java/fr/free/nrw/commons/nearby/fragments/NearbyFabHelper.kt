package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.utils.NearbyFABUtils.addAnchorToBigFABs
import fr.free.nrw.commons.utils.NearbyFABUtils.addAnchorToSmallFABs
import fr.free.nrw.commons.utils.NearbyFABUtils.removeAnchorFromFAB

private const val ALPHA_TRANSPARENT = 0f
private const val LOGIN_SKIPPED_KEY = "login_skipped"

/**
 * Manages floating action buttons (FABs), speed-dial animations, CoordinatorLayout anchoring,
 * and RTL/LTR horizontal spread layouts for NearbyParentFragment.
 */
class NearbyFabHelper : NearbyParentFragmentContract.FabControl {
    /**
     * Interface defining callbacks for fragment-level actions triggered by FAB operations.
     */
    interface NearbyFabActions {
        /** Returns true if the fragment is currently in depicts picker mode. */
        fun isDepictsPickerMode(): Boolean

        /** Invoked when FAB recenter is disabled to add photo markers if in depicts mode. */
        fun onPhotoMarkerCheckRequested()
    }

    @VisibleForTesting
    var rotateBackward: Animation? = null

    @VisibleForTesting
    var fabClose: Animation? = null

    @VisibleForTesting
    var fabOpen: Animation? = null

    @VisibleForTesting
    var rotateForward: Animation? = null

    @VisibleForTesting
    var isFABsExpanded: Boolean = false

    @VisibleForTesting
    var bindingProvider: (() -> FragmentNearbyParentBinding?)? = null

    @VisibleForTesting
    var contextProvider: (() -> Context?)? = null

    @VisibleForTesting
    var activityProvider: (() -> Activity?)? = null

    @VisibleForTesting
    var applicationKvStoreProvider: (() -> JsonKvStore?)? = null

    @VisibleForTesting
    var actions: NearbyFabActions? = null

    /**
     * Initializes FAB animations, providers, and callback actions.
     */
    fun init(
        bindingProvider: () -> FragmentNearbyParentBinding?,
        contextProvider: () -> Context?,
        activityProvider: () -> Activity?,
        applicationKvStoreProvider: () -> JsonKvStore?,
        actions: NearbyFabActions,
    ) {
        this.bindingProvider = bindingProvider
        this.contextProvider = contextProvider
        this.activityProvider = activityProvider
        this.applicationKvStoreProvider = applicationKvStoreProvider
        this.actions = actions

        val context = contextProvider() ?: return
        loadAnimations(this, context)
    }

    /**
     * Starts animation of fab plus (turning on opening) and other FABs
     */
    override fun animateFABs() {
        val binding = bindingProvider?.invoke() ?: return
        if (binding.fabPlus.isShown) {
            if (isFABsExpanded) {
                collapseFABs(this, binding)
            } else {
                expandFABs(this, binding)
            }
        }
    }

    /**
     * onLogoutComplete is called after shared preferences and data stored in local database are
     * cleared.
     */
    override fun displayLoginSkippedWarning() {
        val kvStore = applicationKvStoreProvider?.invoke()
        val context = contextProvider?.invoke()
        val activity = activityProvider?.invoke()
        if (kvStore == null || context == null || activity == null) {
            return
        }
        if (kvStore.getBoolean(LOGIN_SKIPPED_KEY, false)) {
            // prompt the user to login
            AlertDialog
                .Builder(context)
                .setMessage(R.string.login_alert_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .setPositiveButton(R.string.login) { _, _ ->
                    // logout of the app
                    val logoutListener = CommonsApplication.BaseLogoutListener(activity)
                    val app = activity.application as CommonsApplication
                    app.clearApplicationData(context, logoutListener)
                }.show()
        }
    }

    override fun setFABPlusAction(onClickListener: View.OnClickListener) {
        bindingProvider?.invoke()?.fabPlus?.setOnClickListener(onClickListener)
    }

    override fun setFABRecenterAction(onClickListener: View.OnClickListener) {
        bindingProvider?.invoke()?.fabRecenter?.setOnClickListener(onClickListener)
    }

    /**
     * The [NearbyParentFragmentPresenter.lockUnlockNearby] disables enables FAB recenter.
     * This lets us [addPhotoMarkerIfDepictsMode] pre-populating before place markers
     * if nearby places found are null and place markers are not rendered
     */
    override fun disableFABRecenter() {
        bindingProvider?.invoke()?.fabRecenter?.isEnabled = false
        actions?.onPhotoMarkerCheckRequested()
    }

    override fun enableFABRecenter() {
        bindingProvider?.invoke()?.fabRecenter?.isEnabled = true
    }

    /**
     * Updates spacer heights, alignment rules, and directional horizontal spread translation.
     */
    fun applyNearbyFabLayout() {
        val binding = bindingProvider?.invoke() ?: return
        val context = contextProvider?.invoke() ?: return
        val res = context.resources
        val spacer1Px = res.getDimensionPixelSize(R.dimen.nearby_fab_spacer1_height)
        val spacer2Px = res.getDimensionPixelSize(R.dimen.nearby_fab_spacer2_height)
        val spacer3Px = res.getDimensionPixelSize(R.dimen.nearby_fab_spacer3_height)
        val spacingPx = res.getDimensionPixelSize(R.dimen.nearby_fab_spread_spacing)
        val marginPx = res.getDimensionPixelSize(R.dimen.nearby_fab_container_margin)
        val alignStart = res.getBoolean(R.bool.nearby_fab_container_align_start)

        applySpacerHeights(binding, spacer1Px, spacer2Px, spacer3Px)
        applyContainerAlignment(binding, alignStart, marginPx)
        applyHorizontalSpread(binding, spacingPx)
    }

    /**
     * If nearby details bottom sheet state is collapsed: show fab plus If nearby details bottom
     * sheet state is expanded: show fab plus If nearby details bottom sheet state is hidden: hide
     * all fabs
     *
     * @param bottomSheetState see bottom sheet states
     */
    override fun prepareViewsForSheetPosition(bottomSheetState: Int) {
        val binding = bindingProvider?.invoke() ?: return
        when (bottomSheetState) {
            BottomSheetBehavior.STATE_COLLAPSED -> {
                collapseFABs(this, binding)
                val isDepictsPicker = actions?.isDepictsPickerMode() ?: false
                if (!binding.fabPlus.isShown && !isDepictsPicker) {
                    showFABs(binding)
                }
            }

            BottomSheetBehavior.STATE_HIDDEN -> {
                binding.transparentView.isClickable = false
                binding.transparentView.alpha = ALPHA_TRANSPARENT
                collapseFABs(this, binding)
                hideFABs(binding)
            }
        }
    }
}

/**
 * Loads animations will be used for FABs
 */
private fun loadAnimations(
    helper: NearbyFabHelper,
    context: Context,
) {
    helper.fabOpen = AnimationUtils.loadAnimation(context, R.anim.fab_open)
    helper.fabClose = AnimationUtils.loadAnimation(context, R.anim.fab_close)
    helper.rotateForward = AnimationUtils.loadAnimation(context, R.anim.rotate_forward)
    helper.rotateBackward = AnimationUtils.loadAnimation(context, R.anim.rotate_backward)
}

private fun showFABs(binding: FragmentNearbyParentBinding) {
    addAnchorToBigFABs(
        binding.fabPlus,
        binding.bottomSheetDetails.root.id,
    )
    binding.fabPlus.show()
    addAnchorToSmallFABs(
        binding.fabGallery,
        binding.emptyView.id,
    )
    addAnchorToSmallFABs(
        binding.fabCamera,
        binding.emptyView1.id,
    )
    addAnchorToSmallFABs(
        binding.fabCustomGallery,
        binding.emptyView2.id,
    )
}

/**
 * Hides all fabs
 */
private fun hideFABs(binding: FragmentNearbyParentBinding) {
    removeAnchorFromFAB(binding.fabPlus)
    binding.fabPlus.hide()
    removeAnchorFromFAB(binding.fabCamera)
    binding.fabCamera.hide()
    removeAnchorFromFAB(binding.fabGallery)
    binding.fabGallery.hide()
    removeAnchorFromFAB(binding.fabCustomGallery)
    binding.fabCustomGallery.hide()
}

/**
 * Expands camera and gallery FABs, turn forward plus FAB
 *
 * @param isFABsExpanded true if they are already expanded
 */
private fun expandFABs(
    helper: NearbyFabHelper,
    binding: FragmentNearbyParentBinding,
) {
    if (!helper.isFABsExpanded) {
        showFABs(binding)
        binding.fabPlus.startAnimation(helper.rotateForward)
        binding.fabCamera.startAnimation(helper.fabOpen)
        binding.fabGallery.startAnimation(helper.fabOpen)
        binding.fabCustomGallery.startAnimation(helper.fabOpen)
        binding.fabCustomGallery.show()
        binding.fabCamera.show()
        binding.fabGallery.show()
        helper.isFABsExpanded = true
    }
}

/**
 * Collapses camera and gallery FABs, turn back plus FAB
 *
 * @param isFABsExpanded
 */
private fun collapseFABs(
    helper: NearbyFabHelper,
    binding: FragmentNearbyParentBinding,
) {
    if (helper.isFABsExpanded) {
        binding.fabPlus.startAnimation(helper.rotateBackward)
        binding.fabCamera.startAnimation(helper.fabClose)
        binding.fabGallery.startAnimation(helper.fabClose)
        binding.fabCustomGallery.startAnimation(helper.fabClose)
        binding.fabCustomGallery.hide()
        binding.fabCamera.hide()
        binding.fabGallery.hide()
        helper.isFABsExpanded = false
    }
}

private fun applySpacerHeights(
    binding: FragmentNearbyParentBinding,
    spacer1Px: Int,
    spacer2Px: Int,
    spacer3Px: Int,
) {
    binding.emptyView1.layoutParams.height = spacer1Px
    binding.emptyView1.requestLayout()
    binding.emptyView.layoutParams.height = spacer2Px
    binding.emptyView.requestLayout()
    binding.emptyView2.layoutParams.height = spacer3Px
    binding.emptyView2.requestLayout()
}

private fun applyContainerAlignment(
    binding: FragmentNearbyParentBinding,
    alignStart: Boolean,
    marginPx: Int,
) {
    binding.fabContainer.updateLayoutParams<RelativeLayout.LayoutParams> {
        if (alignStart) {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            removeRule(RelativeLayout.ALIGN_PARENT_END)
            marginStart = marginPx
            marginEnd = 0
        } else {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            removeRule(RelativeLayout.ALIGN_PARENT_START)
            marginStart = 0
            marginEnd = marginPx
        }
    }
}

private fun applyHorizontalSpread(
    binding: FragmentNearbyParentBinding,
    spacingPx: Int,
) {
    if (spacingPx == 0) {
        binding.fabCamera.translationX = 0f
        binding.fabGallery.translationX = 0f
        binding.fabCustomGallery.translationX = 0f
    } else {
        val direction = if (binding.root.layoutDirection == View.LAYOUT_DIRECTION_RTL) 1 else -1
        listOf(
            binding.fabCustomGallery,
            binding.fabGallery,
            binding.fabCamera,
        ).forEachIndexed { index, fab ->
            fab.translationX = direction * spacingPx * (index + 1f)
        }
    }
}
