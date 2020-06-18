package fr.free.nrw.commons.explore.depictions.child

import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject

class PageableChildDepictionsDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    private val okHttpJsonApiClient: OkHttpJsonApiClient
) : PageableBaseDataSource<DepictedItem>(liveDataConverter) {
    override val loadFunction = { _: Int, startPosition: Int ->
        if (startPosition == 0) okHttpJsonApiClient.getChildDepictions(query).blockingFirst()
        else emptyList()
    }
}

