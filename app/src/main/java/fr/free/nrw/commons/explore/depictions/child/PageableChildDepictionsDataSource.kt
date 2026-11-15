package fr.free.nrw.commons.explore.depictions.child

import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.mwapi.WikidataQueryClient
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject

class PageableChildDepictionsDataSource
    @Inject
    constructor(
        liveDataConverter: LiveDataConverter,
        private val wikidataQueryClient: WikidataQueryClient,
    ) : PageableBaseDataSource<DepictedItem>(liveDataConverter) {
        override val loadFunction = { limit: Int, startPosition: Int ->
            wikidataQueryClient.getChildDepictions(query, startPosition, limit).blockingGet()
        }
    }
