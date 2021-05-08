package fr.free.nrw.commons.nearby

import android.view.View
import android.view.View.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.hannesdorfmann.adapterdelegates4.dsl.AdapterDelegateLayoutContainerViewHolder
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegateLayoutContainer
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import kotlinx.android.synthetic.main.item_place.*
import kotlinx.android.synthetic.main.nearby_row_button.*


fun placeAdapterDelegate(
    bookmarkLocationDao: BookmarkLocationsDao,
    onItemClick: ((Place) -> Unit)? = null,
    onCameraClicked: (Place) -> Unit,
    onGalleryClicked: (Place) -> Unit,
    onBookmarkClicked: (Place, Boolean) -> Unit,
    onOverflowIconClicked: (Place, View) -> Unit,
    onDirectionsClicked: (Place) -> Unit
) =
    adapterDelegateLayoutContainer<Place, Place>(R.layout.item_place) {
        containerView.setOnClickListener { _: View? ->
            showOrHideAndScrollToIfLast()
            onItemClick?.invoke(item)
        }
        containerView.setOnFocusChangeListener { view1: View?, hasFocus: Boolean ->
            if (!hasFocus && buttonLayout.isShown) {
                buttonLayout.visibility = GONE
            } else if (hasFocus && !buttonLayout.isShown) {
                showOrHideAndScrollToIfLast()
                onItemClick?.invoke(item)
            }
        }
        cameraButton.setOnClickListener { onCameraClicked(item) }
        galleryButton.setOnClickListener { onGalleryClicked(item) }
        bookmarkButtonImage.setOnClickListener {
            val isBookmarked = bookmarkLocationDao.updateBookmarkLocation(item)
            bookmarkButtonImage.setImageResource(if (isBookmarked) R.drawable.ic_round_star_filled_24px else R.drawable.ic_round_star_border_24px)
            onBookmarkClicked(item, isBookmarked)
        }
        iconOverflow.setOnClickListener { onOverflowIconClicked(item, it) }
        directionsButton.setOnClickListener { onDirectionsClicked(item) }
        bind {
            tvName.text = item.name
            val descriptionText: String = item.longDescription
            if (descriptionText == "?") {
                tvDesc.setText(R.string.no_description_found)
                tvDesc.visibility = INVISIBLE
            } else {
                tvDesc.text = descriptionText.replace("\\(.*?\\)","");
            }
            distance.text = item.distance
            icon.setImageResource(item.label.icon)
            iconOverflow.visibility =
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

private fun AdapterDelegateLayoutContainerViewHolder<Place>.showOrHideAndScrollToIfLast() {
    TransitionManager.beginDelayedTransition(buttonLayout)
    if (buttonLayout.isShown) {
        buttonLayout.visibility = GONE
    } else {
        buttonLayout.visibility = VISIBLE
        val recyclerView = containerView.parent as RecyclerView
        val lastPosition = recyclerView.adapter!!.itemCount - 1
        if (recyclerView.getChildLayoutPosition(containerView) == lastPosition) {
            (recyclerView.layoutManager as LinearLayoutManager?)
                ?.scrollToPositionWithOffset(lastPosition, buttonLayout.height)
        }
    }
}
