package fr.free.nrw.commons.explore.depictions.parent

import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import javax.inject.Inject

class PageableParentDepictionsDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) : PageableBaseDataSource<DepictedItem>(liveDataConverter) {
    override val loadFunction = { limit: Int, startPosition: Int ->
        okHttpJsonApiClient.getParentDepictions(query, startPosition, limit).blockingGet()
    }
}

