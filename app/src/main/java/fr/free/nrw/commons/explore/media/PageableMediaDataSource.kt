package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.data.models.Media
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.explore.depictions.search.LoadFunction
import fr.free.nrw.commons.media.MediaClient
import javax.inject.Inject

class PageableMediaDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val mediaClient: MediaClient
) : PageableBaseDataSource<Media>(liveDataConverter) {
    override val loadFunction: LoadFunction<Media> = { loadSize: Int, startPosition: Int ->
        mediaClient.getMediaListFromSearch(query, loadSize, startPosition).blockingGet()
    }
}
