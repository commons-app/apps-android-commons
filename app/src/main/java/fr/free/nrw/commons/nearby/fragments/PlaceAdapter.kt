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
            commonPlaceClickActions.onGalleryClicked(),
            onBookmarkClicked,
            commonPlaceClickActions.onOverflowClicked(),
            commonPlaceClickActions.onDirectionsClicked(),
            inAppCameraLocationPermissionLauncher
        ),
        areItemsTheSame = {oldItem, newItem -> oldItem.wikiDataEntityId == newItem.wikiDataEntityId }
    )
