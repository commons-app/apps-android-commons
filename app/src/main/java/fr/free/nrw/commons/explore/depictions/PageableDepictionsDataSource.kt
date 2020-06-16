package fr.free.nrw.commons.explore.depictions

import fr.free.nrw.commons.explore.LiveDataConverter
import fr.free.nrw.commons.explore.LoadingState
import fr.free.nrw.commons.explore.PageableDataSource
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

typealias LoadFunction<T> = (Int, Int) -> List<T>
typealias LoadingStates = PublishProcessor<LoadingState>

class PageableDepictionsDataSource @Inject constructor(
    liveDataConverter: LiveDataConverter,
    val depictsClient: DepictsClient
) : PageableDataSource<DepictedItem>(liveDataConverter) {

    override val loadFunction = { loadSize: Int, startPosition: Int ->
        depictsClient.searchForDepictions(query, loadSize, startPosition).blockingGet()
    }
}

