package fr.free.nrw.commons.explore.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.depictions.Media.DepictedImagesFragment.PAGE_ID_PREFIX
import fr.free.nrw.commons.explore.LiveDataConverter
import fr.free.nrw.commons.explore.PageableDataSource
import fr.free.nrw.commons.explore.depictions.LoadFunction
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.media.MediaClient.Companion.NO_CAPTION
import javax.inject.Inject

class PageableMediaDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val mediaConverter: MediaConverter,
    private val mediaClient: MediaClient
) : PageableDataSource<Media>(liveDataConverter) {
    override val loadFunction: LoadFunction<Media> = { loadSize: Int, startPosition: Int ->
        mediaClient.getMediaListFromSearch(query, loadSize, startPosition)
            .map { it.query()?.pages()?.map(mediaConverter::convert) ?: emptyList() }
            .map { it.zip(getCaptions(it)) }
            .map { it.map { (media, caption) -> media.also { it.caption = caption } } }
            .blockingGet()
    }

    private fun getCaptions(it: List<Media>) =
        mediaClient.getEntities(it.joinToString("|") { PAGE_ID_PREFIX + it.pageId })
            .map {
                it.entities().values.map { entity ->
                    entity.labels().values.firstOrNull()?.value() ?: NO_CAPTION
                }
            }
            .blockingGet()

}
