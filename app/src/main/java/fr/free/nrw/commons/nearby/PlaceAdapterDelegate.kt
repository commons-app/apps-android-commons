package fr.free.nrw.commons.nearby

import android.content.Intent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.databinding.ItemPlaceBinding
import kotlinx.coroutines.launch

fun placeAdapterDelegate(
    bookmarkLocationDao: BookmarkLocationsDao,
    scope: LifecycleCoroutineScope?,
    onItemClick: ((Place) -> Unit)? = null,
    onCameraClicked: (Place, ActivityResultLauncher<Array<String>>, ActivityResultLauncher<Intent>) -> Unit,
    onCameraLongPressed: () -> Boolean,
    onGalleryClicked: (Place, ActivityResultLauncher<Intent>) -> Unit,
    onGalleryLongPressed: () -> Boolean,
    onBookmarkClicked: (Place, Boolean) -> Unit,
    onBookmarkLongPressed: () -> Boolean,
    onOverflowIconClicked: (Place, View) -> Unit,
    onOverFlowLongPressed: () -> Boolean,
    onDirectionsClicked: (Place) -> Unit,
    onDirectionsLongPressed: () -> Boolean,
    inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>,
    cameraPickLauncherForResult: ActivityResultLauncher<Intent>,
    galleryPickLauncherForResult: ActivityResultLauncher<Intent>
) = adapterDelegateViewBinding<Place, Place, ItemPlaceBinding>({ layoutInflater, parent ->
    ItemPlaceBinding.inflate(layoutInflater, parent, false)
}) {
    with(binding) {
        root.setOnClickListener { _: View? ->
            showOrHideAndScrollToIfLast()
            onItemClick?.invoke(item)
        }
        root.setOnFocusChangeListener { _: View?, hasFocus: Boolean ->
            val parentView = root.parent.parent.parent as? RelativeLayout
            val bottomSheetBehavior = parentView?.let { BottomSheetBehavior.from(it) }

            // Hide button layout if focus is lost, otherwise show it if it's not already visible
            if (!hasFocus && nearbyButtonLayout.buttonLayout.isShown) {
                nearbyButtonLayout.buttonLayout.visibility = GONE
            } else if (hasFocus && !nearbyButtonLayout.buttonLayout.isShown) {
                if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                    showOrHideAndScrollToIfLast()
                    onItemClick?.invoke(item)
                }
            }
        }
        nearbyButtonLayout.cameraButton.setOnClickListener { onCameraClicked(item, inAppCameraLocationPermissionLauncher, cameraPickLauncherForResult) }
        nearbyButtonLayout.cameraButton.setOnLongClickListener { onCameraLongPressed() }

        nearbyButtonLayout.galleryButton.setOnClickListener { onGalleryClicked(item, galleryPickLauncherForResult) }
        nearbyButtonLayout.galleryButton.setOnLongClickListener { onGalleryLongPressed() }
        bookmarkButtonImage.setOnClickListener {
            var isBookmarked = false
            scope?.launch {
                isBookmarked = bookmarkLocationDao.updateBookmarkLocation(item)
            }
            bookmarkButtonImage.setImageResource(
                if (isBookmarked) R.drawable.ic_round_star_filled_24px else R.drawable.ic_round_star_border_24px,
            )
            onBookmarkClicked(item, isBookmarked)
        }
        bookmarkButtonImage.setOnLongClickListener { onBookmarkLongPressed() }
        nearbyButtonLayout.iconOverflow.setOnClickListener { onOverflowIconClicked(item, it) }
        nearbyButtonLayout.iconOverflow.setOnLongClickListener { onOverFlowLongPressed() }
        nearbyButtonLayout.directionsButton.setOnClickListener { onDirectionsClicked(item) }
        bind {
            tvName.text = item.name
            val descriptionText: String = item.longDescription!!
            if (descriptionText == "?") {
                tvDesc.setText(R.string.no_description_found)
                tvDesc.visibility = INVISIBLE
            } else {
                // Remove the label and display only texts inside pharentheses (description) since too long
                tvDesc.text =
                    descriptionText
                        .substringAfter(tvName.text.toString() + " (")
                        .substringBeforeLast(")")
            }
            distance.text = item.distance
            icon.setImageResource(item.label!!.icon)
            nearbyButtonLayout.iconOverflow.visibility =
                if (item.hasCommonsLink() || item.hasWikidataLink()) {
                    VISIBLE
                } else {
                    GONE
                }

            scope?.launch {
                bookmarkButtonImage.setImageResource(
                    if (bookmarkLocationDao.findBookmarkLocation(item.name!!)) {
                        R.drawable.ic_round_star_filled_24px
                    } else {
                        R.drawable.ic_round_star_border_24px
                    },
                )
            }
        }
        nearbyButtonLayout.directionsButton.setOnLongClickListener { onDirectionsLongPressed() }
    }
}

private fun AdapterDelegateViewBindingViewHolder<Place, ItemPlaceBinding>.showOrHideAndScrollToIfLast() {
    with(binding) {
        TransitionManager.beginDelayedTransition(nearbyButtonLayout.buttonLayout)
        if (nearbyButtonLayout.buttonLayout.isShown) {
            nearbyButtonLayout.buttonLayout.visibility = GONE
        } else {
            nearbyButtonLayout.buttonLayout.visibility = VISIBLE
            val recyclerView = root.parent as RecyclerView
            val lastPosition = recyclerView.adapter!!.itemCount - 1
            if (recyclerView.getChildLayoutPosition(root) == lastPosition) {
                (recyclerView.layoutManager as LinearLayoutManager?)
                    ?.scrollToPositionWithOffset(
                        lastPosition,
                        nearbyButtonLayout.buttonLayout.height,
                    )
            }
        }
    }
}
