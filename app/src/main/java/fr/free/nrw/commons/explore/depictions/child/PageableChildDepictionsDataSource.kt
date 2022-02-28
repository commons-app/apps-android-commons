package fr.free.nrw.commons.explore.depictions.child

import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import javax.inject.Inject

class PageableChildDepictionsDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) : PageableBaseDataSource<DepictedItem>(liveDataConverter) {
    override val loadFunction = { limit: Int, startPosition: Int ->
        okHttpJsonApiClient.getChildDepictions(query, startPosition, limit).blockingGet()
    }
}

