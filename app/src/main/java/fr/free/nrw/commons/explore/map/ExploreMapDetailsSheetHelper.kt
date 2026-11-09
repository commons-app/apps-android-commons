package fr.free.nrw.commons.explore.map

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.databinding.BottomSheetDetailsExploreBinding
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.handleGeoCoordinates
import fr.free.nrw.commons.utils.handleWebUrl
import javax.inject.Inject

/**
 * Manages the presentation, interaction callbacks, and visibility states of the Explore
 * map bottom sheet details view.
 */
class ExploreMapDetailsSheetHelper
    @Inject
    constructor() :
    ExploreMapContract.BottomSheet,
        ExploreMapDetailsSheetActions {
        @VisibleForTesting
        var bottomSheetDetailsBehavior: BottomSheetBehavior<*>? = null

        @VisibleForTesting
        var binding: BottomSheetDetailsExploreBinding? = null

        /**
         * Initializes the bottom sheet behavior and visibility.
         *
         * @param detailsBinding The binding for the bottom sheet details layout.
         */
        fun init(detailsBinding: BottomSheetDetailsExploreBinding) {
            this.binding = detailsBinding
            bottomSheetDetailsBehavior =
                BottomSheetBehavior.from<LinearLayout?>(
                    detailsBinding.root,
                )
            bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
            detailsBinding.root.visibility = View.VISIBLE
            setBottomSheetCallbacks()
        }

        /**
         * Defines how bottom sheets will act on click
         */
        private fun setBottomSheetCallbacks() {
            binding?.root?.setOnClickListener {
                if (bottomSheetDetailsBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
                } else if (bottomSheetDetailsBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }

        /**
         * Hides the bottom details sheet if it is currently displayed.
         */
        override fun hideBottomDetailsSheet() {
            bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        }

        /**
         * Checks whether the details bottom sheet is currently visible.
         *
         * @return true if visible, false otherwise
         */
        override fun isDetailsBottomSheetVisible(): Boolean = binding?.root?.isVisible == true

        /**
         * Handles the back button event to collapse or hide the bottom sheet if expanded.
         *
         * @return true if the back press was handled by the bottom sheet, false otherwise
         */
        override fun backButtonClicked(): Boolean {
            if (bottomSheetDetailsBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            return false
        }

        /**
         * Shows the details sheet in collapsed state when a marker is clicked.
         */
        fun showDetails() {
            bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
            bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        /**
         * Handles map tap events to collapse or hide the bottom sheet.
         *
         * @return true if sheet state was changed, false otherwise
         */
        fun handleMapTap(): Boolean {
            if (bottomSheetDetailsBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                // Back should first hide the bottom sheet if it is expanded
                bottomSheetDetailsBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
                return true
            }
            val wasVisible = isDetailsBottomSheetVisible()
            if (wasVisible) {
                hideBottomDetailsSheet()
            }
            return wasVisible
        }

        /**
         * Same bottom sheet carries information for all nearby places, so we need to pass information
         * (title, description, distance and links) to view on nearby marker click
         *
         * @param context Android context for launching external intents
         * @param place Place of clicked nearby marker
         * @param mediaList Media collection for matching image detail navigation
         * @param zoomLevel Current zoom level of the map
         * @param onMediaClicked Callback invoked when media details button is clicked
         */
        fun passInfoToSheet(
            context: Context,
            place: Place,
            mediaList: List<Media>?,
            zoomLevel: Double,
            onMediaClicked: (Int) -> Unit,
        ) {
            val detailsBinding = binding ?: return
            detailsBinding.directionsButton.setOnClickListener {
                handleGeoCoordinates(context, place.getLocation(), zoomLevel)
            }

            detailsBinding.commonsButton.visibility =
                if (place.hasCommonsLink()) View.VISIBLE else View.GONE
            detailsBinding.commonsButton.setOnClickListener {
                handleWebUrl(context, place.siteLinks.commonsLink)
            }

            mediaList?.forEachIndexed { index, media ->
                if (media.filename == place.name) {
                    detailsBinding.mediaDetailsButton.setOnClickListener {
                        onMediaClicked(index)
                    }
                }
            }
            detailsBinding.title.text = place.name.removePrefix("File:").substringBeforeLast(".")
            detailsBinding.category.text = place.distance
            // Remove label since it is double information
            var descriptionText =
                place.longDescription
                    .replace(place.getName() + " (", "")
            descriptionText = (
                if (descriptionText == place.longDescription) {
                    descriptionText
                } else {
                    descriptionText.replaceFirst(".$".toRegex(), "")
                }
            )
            // Set the short description after we remove place name from long description
            detailsBinding.description.text = descriptionText
        }
    }
