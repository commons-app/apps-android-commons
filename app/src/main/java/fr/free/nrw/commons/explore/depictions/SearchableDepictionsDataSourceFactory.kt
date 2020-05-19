package fr.free.nrw.commons.explore.depictions

import androidx.lifecycle.LiveData
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.toLiveData
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

private const val PAGE_SIZE = 50
private const val INITIAL_LOAD_SIZE = 50

class SearchableDepictionsDataSourceFactory @Inject constructor(val searchDepictionsDataSourceFactoryFactory: SearchDepictionsDataSourceFactoryFactory) {
    private val _loadingStates = PublishProcessor.create<LoadingState>()
    val loadingStates: Flowable<LoadingState> = _loadingStates
    private val _searchResults = PublishProcessor.create<LiveData<PagedList<DepictedItem>>>()
    val searchResults: Flowable<LiveData<PagedList<DepictedItem>>> = _searchResults
    private val _noItemsLoadedEvent = PublishProcessor.create<Unit>()
    val noItemsLoadedEvent: Flowable<Unit> = _noItemsLoadedEvent

    var currentFactory: SearchDepictionsDataSourceFactory? = null

    fun onQueryUpdated(query: String) {
        _searchResults.offer(
            searchDepictionsDataSourceFactoryFactory.create(query, _loadingStates)
                .also { currentFactory = it }
                .toLiveData(
                    Config(
                        pageSize = PAGE_SIZE,
                        initialLoadSizeHint = INITIAL_LOAD_SIZE,
                        enablePlaceholders = false
                    ),
                    boundaryCallback = object : PagedList.BoundaryCallback<DepictedItem>() {
                        override fun onZeroItemsLoaded() {
                            _noItemsLoadedEvent.offer(Unit)
                        }
                    }
                )
        )
    }

    fun retryFailedRequest() {
        currentFactory?.retryFailedRequest()
    }
}

interface SearchDepictionsDataSourceFactoryFactory {
    fun create(query: String, loadingStates: PublishProcessor<LoadingState>)
            : SearchDepictionsDataSourceFactory
}

class SearchDepictionsDataSourceFactory constructor(
    private val depictsClient: DepictsClient,
    private val query: String,
    private val loadingStates: PublishProcessor<LoadingState>
) : DataSource.Factory<Int, DepictedItem>() {
    var currentDataSource: SearchDepictionsDataSource? = null
    override fun create() = SearchDepictionsDataSource(depictsClient, loadingStates, query).also {
        currentDataSource = it
    }

    fun retryFailedRequest() {
        currentDataSource?.retryFailedRequest()
    }
}

sealed class LoadingState {
    object InitialLoad : LoadingState()
    object Loading : LoadingState()
    object Complete : LoadingState()
    object Error : LoadingState()
}
