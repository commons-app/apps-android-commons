package fr.free.nrw.commons.explore.categories.media

import fr.free.nrw.commons.models.Media
import fr.free.nrw.commons.explore.depictions.search.LoadFunction
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.media.MediaClient
import javax.inject.Inject

class PageableCategoriesMediaDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val mediaClient: MediaClient
) : PageableBaseDataSource<Media>(liveDataConverter) {
    override val loadFunction: LoadFunction<Media> = { loadSize: Int, startPosition: Int ->
        if(startPosition == 0){
            mediaClient.resetCategoryContinuation(query)
        }
        mediaClient.getMediaListFromCategory(query).blockingGet()
    }
}
