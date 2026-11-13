package fr.free.nrw.commons.explore.categories.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.depictions.search.LoadFunction
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.media.MediaCollectionClient
import javax.inject.Inject

class PageableCategoriesMediaDataSource
    @Inject
    constructor(
        liveDataConverter: LiveDataConverter,
        private val mediaCollectionClient: MediaCollectionClient,
    ) : PageableBaseDataSource<Media>(liveDataConverter) {
        override val loadFunction: LoadFunction<Media> = { loadSize: Int, startPosition: Int ->
            if (startPosition == 0) {
                mediaCollectionClient.resetCategoryContinuation(query)
            }
            mediaCollectionClient.getMediaListFromCategory(query).blockingGet()
        }
    }
