package fr.free.nrw.commons.media

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.explore.media.MediaConverter
import fr.free.nrw.commons.wikidata.model.Entities
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import io.reactivex.Single
import javax.inject.Inject

class MediaResponseMapper
    @Inject
    constructor(
        private val mediaDetailInterface: MediaDetailInterface,
        private val mediaConverter: MediaConverter,
    ) {
        fun map(networkResult: Single<MwQueryResponse>): Single<List<Media>> =
            networkResult
                .map { it.query()?.pages() ?: emptyList() }
                .flatMap(::mediaFromPageAndEntity)

        fun getEntities(entityIds: List<String>): Single<Entities> =
            if (entityIds.isEmpty()) {
                Single.error(Exception("empty list passed for ids"))
            } else {
                mediaDetailInterface.getEntity(entityIds.joinToString("|"))
            }

        private fun mediaFromPageAndEntity(pages: List<MwQueryPage>): Single<List<Media>> =
            when {
                pages.isEmpty() -> Single.just(emptyList())
                pagesIndicateDeletedFile(pages) -> Single.error(FileDeletedOnCommonsException())
                else ->
                    getEntities(pages.map { "$PAGE_ID_PREFIX${it.pageId()}" })
                        .map {
                            pages
                                .zip(it.getEntityMap().values)
                                .mapNotNull { (page, entity) ->
                                    page.imageInfo()?.let {
                                        mediaConverter.convert(page, entity, it)
                                    }
                                }
                        }
            }
    }
