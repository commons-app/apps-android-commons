package fr.free.nrw.commons.nearby.fragments

import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.placeAdapterDelegate
import fr.free.nrw.commons.upload.categories.BaseDelegateAdapter

class PlaceAdapter(
    bookmarkLocationsDao: BookmarkLocationsDao,
    onPlaceClicked: ((Place) -> Unit)? = null,
    onBookmarkClicked: (Place, Boolean) -> Unit,
    commonPlaceClickActions: CommonPlaceClickActions
) :
    BaseDelegateAdapter<Place>(
        placeAdapterDelegate(
            bookmarkLocationsDao,
            onPlaceClicked,
            commonPlaceClickActions.onCameraClicked(),
            commonPlaceClickActions.onGalleryClicked(),
            onBookmarkClicked,
            commonPlaceClickActions.onOverflowClicked(),
            commonPlaceClickActions.onDirectionsClicked()
        ),
        areItemsTheSame = {oldItem, newItem -> oldItem.wikiDataEntityId == newItem.wikiDataEntityId }
    )
