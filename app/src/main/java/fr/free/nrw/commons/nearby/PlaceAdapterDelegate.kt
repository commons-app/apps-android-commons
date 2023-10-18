package fr.free.nrw.commons.nearby

import android.view.View
import android.view.View.*
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateViewBindingViewHolder
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateViewBinding
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.databinding.ItemPlaceBinding


fun placeAdapterDelegate(
    bookmarkLocationDao: BookmarkLocationsDao,
    onItemClick: ((Place) -> Unit)? = null,
    onCameraClicked: (Place, ActivityResultLauncher<Array<String>>) -> Unit,
    onGalleryClicked: (Place) -> Unit,
    onBookmarkClicked: (Place, Boolean) -> Unit,
    onOverflowIconClicked: (Place, View) -> Unit,
    onDirectionsClicked: (Place) -> Unit,
    inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>
) = adapterDelegateViewBinding<Place, Place, ItemPlaceBinding>({ layoutInflater, parent ->
    ItemPlaceBinding.inflate(layoutInflater, parent, false)
}) {
    with(binding) {
        root.setOnClickListener { _: View? ->
            showOrHideAndScrollToIfLast()
            onItemClick?.invoke(item)
        }
        root.setOnFocusChangeListener { view1: View?, hasFocus: Boolean ->
            if (!hasFocus && nearbyButtonLayout.buttonLayout.isShown) {
                nearbyButtonLayout.buttonLayout.visibility = GONE
            } else if (hasFocus && !nearbyButtonLayout.buttonLayout.isShown) {
                showOrHideAndScrollToIfLast()
                onItemClick?.invoke(item)
            }
        }
        nearbyButtonLayout.cameraButton.setOnClickListener { onCameraClicked(item, inAppCameraLocationPermissionLauncher) }
        nearbyButtonLayout.galleryButton.setOnClickListener { onGalleryClicked(item) }
        bookmarkButtonImage.setOnClickListener {
            val isBookmarked = bookmarkLocationDao.updateBookmarkLocation(item)
            bookmarkButtonImage.setImageResource(
                if (isBookmarked) R.drawable.ic_round_star_filled_24px else R.drawable.ic_round_star_border_24px
            )
            onBookmarkClicked(item, isBookmarked)
        }
        nearbyButtonLayout.iconOverflow.setOnClickListener { onOverflowIconClicked(item, it) }
        nearbyButtonLayout.directionsButton.setOnClickListener { onDirectionsClicked(item) }
        bind {
            tvName.text = item.name
            val descriptionText: String = item.longDescription
            if (descriptionText == "?") {
                tvDesc.setText(R.string.no_description_found)
                tvDesc.visibility = INVISIBLE
            } else {
                // Remove the label and display only texts inside pharentheses (description) since too long
                tvDesc.text =
                    descriptionText.substringAfter(tvName.text.toString() + " (")
                        .substringBeforeLast(")");
            }
            distance.text = item.distance
            icon.setImageResource(item.label.icon)
            nearbyButtonLayout.iconOverflow.visibility =
                if (item.hasCommonsLink() || item.hasWikidataLink()) VISIBLE
                else GONE

            bookmarkButtonImage.setImageResource(
                if (bookmarkLocationDao.findBookmarkLocation(item))
                    R.drawable.ic_round_star_filled_24px
                else
                    R.drawable.ic_round_star_border_24px
            )
        }
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
                        nearbyButtonLayout.buttonLayout.height
                    )
            }
        }
    }
}
