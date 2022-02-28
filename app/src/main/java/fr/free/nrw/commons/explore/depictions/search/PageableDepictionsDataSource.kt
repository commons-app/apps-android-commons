package fr.free.nrw.commons.explore.depictions.search

import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import fr.free.nrw.commons.explore.paging.LiveDataConverter
import fr.free.nrw.commons.explore.paging.LoadingState
import fr.free.nrw.commons.explore.paging.PageableBaseDataSource
import fr.free.nrw.commons.explore.depictions.DepictsClient
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

typealias LoadFunction<T> = (Int, Int) -> List<T>
typealias LoadingStates = PublishProcessor<LoadingState>

class PageableDepictionsDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    val depictsClient: DepictsClient
) : PageableBaseDataSource<DepictedItem>(liveDataConverter) {

    override val loadFunction =  { loadSize: Int, startPosition: Int ->
        depictsClient.searchForDepictions(query, loadSize, startPosition).blockingGet()
    }
}

