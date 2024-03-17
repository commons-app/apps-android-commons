package fr.free.nrw.commons.nearby.fragments

import androidx.activity.result.ActivityResultLauncher
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.placeAdapterDelegate
import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter

class PlaceAdapter(
    bookmarkLocationsDao: BookmarkLocationsDao,
    onPlaceClicked: ((Place) -> Unit)? = null,
    onBookmarkClicked: (Place, Boolean) -> Unit,
    commonPlaceClickActions: CommonPlaceClickActions,
    inAppCameraLocationPermissionLauncher: ActivityResultLauncher<Array<String>>
) :
    BaseDelegateAdapter<Place>(
        placeAdapterDelegate(
            bookmarkLocationsDao,
            onPlaceClicked,
            commonPlaceClickActions.onCameraClicked(),
            commonPlaceClickActions.onCameraLongPressed(),
            commonPlaceClickActions.onGalleryClicked(),
            commonPlaceClickActions.onGalleryLongPressed(),
            onBookmarkClicked,
            commonPlaceClickActions.onBookmarkLongPressed(),
            commonPlaceClickActions.onOverflowClicked(),
            commonPlaceClickActions.onOverflowLongPressed(),
            commonPlaceClickActions.onDirectionsClicked(),
            commonPlaceClickActions.onDirectionsLongPressed(),
            inAppCameraLocationPermissionLauncher
        ),
        areItemsTheSame = {oldItem, newItem -> oldItem.wikiDataEntityId == newItem.wikiDataEntityId }
    )
