package fr.free.nrw.commons.explore

import androidx.lifecycle.LiveData
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.PagedList
import androidx.paging.toLiveData
import fr.free.nrw.commons.explore.depictions.LoadFunction
import fr.free.nrw.commons.explore.depictions.LoadingStates
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject

private const val PAGE_SIZE = 50
private const val INITIAL_LOAD_SIZE = 50

abstract class PageableDataSource<T>(private val liveDataConverter: LiveDataConverter) {

    lateinit var query: String
    private val dataSourceFactoryFactory: () -> SearchDataSourceFactory<T> = {
        dataSourceFactory(_loadingStates, loadFunction)
    }
    private val _loadingStates = PublishProcessor.create<LoadingState>()
    val loadingStates: Flowable<LoadingState> = _loadingStates
    private val _searchResults = PublishProcessor.create<LiveData<PagedList<T>>>()
    val searchResults: Flowable<LiveData<PagedList<T>>> = _searchResults
    private val _noItemsLoadedEvent = PublishProcessor.create<String>()
    val noItemsLoadedQueries: Flowable<String> = _noItemsLoadedEvent
    private var currentFactory: SearchDataSourceFactory<T>? = null

    abstract val loadFunction: LoadFunction<T>

    fun onQueryUpdated(query: String) {
        this.query = query
        _searchResults.offer(
            liveDataConverter.convert(dataSourceFactoryFactory().also { currentFactory = it }) {
                _noItemsLoadedEvent.offer(query)
            }
        )
    }

    fun retryFailedRequest() {
        currentFactory?.retryFailedRequest()
    }
}

class LiveDataConverter @Inject constructor() {
    fun <T> convert(
        dataSourceFactory: SearchDataSourceFactory<T>,
        zeroItemsLoadedFunction: () -> Unit
    ): LiveData<PagedList<T>> {
        return dataSourceFactory.toLiveData(
            Config(
                pageSize = PAGE_SIZE,
                initialLoadSizeHint = INITIAL_LOAD_SIZE,
                enablePlaceholders = false
            ),
            boundaryCallback = object : PagedList.BoundaryCallback<T>() {
                override fun onZeroItemsLoaded() {
                    zeroItemsLoadedFunction()
                }
            }
        )
    }

}

abstract class SearchDataSourceFactory<T>(val loadingStates: LoadingStates) :
    DataSource.Factory<Int, T>() {
    private var currentDataSource: SearchDataSource<T>? = null
    abstract val loadFunction: LoadFunction<T>

    override fun create() =
        dataSource(loadingStates, loadFunction).also { currentDataSource = it }

    fun retryFailedRequest() {
        currentDataSource?.retryFailedRequest()
    }

}

fun <T> dataSourceFactory(loadingStates: LoadingStates, loadFunction: LoadFunction<T>) =
    object : SearchDataSourceFactory<T>(loadingStates) {
        override val loadFunction: LoadFunction<T> = loadFunction
    }

sealed class LoadingState {
    object InitialLoad : LoadingState()
    object Loading : LoadingState()
    object Complete : LoadingState()
    object Error : LoadingState()
}
