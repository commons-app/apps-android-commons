package fr.free.nrw.commons.depictions.Media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.LiveDataConverter
import fr.free.nrw.commons.explore.PageableDataSource
import fr.free.nrw.commons.explore.depictions.LoadFunction
import fr.free.nrw.commons.media.MediaClient
import javax.inject.Inject

class PageableDepictedMediaDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val mediaClient: MediaClient
) : PageableDataSource<Media>(liveDataConverter) {
    override val loadFunction: LoadFunction<Media> = { loadSize: Int, startPosition: Int ->
        mediaClient.fetchImagesForDepictedItem(query, loadSize, startPosition).blockingGet()
    }
}
