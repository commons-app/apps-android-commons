package fr.free.nrw.commons.explore

import androidx.paging.PositionalDataSource
import fr.free.nrw.commons.explore.depictions.LoadFunction
import io.reactivex.Completable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

abstract class SearchDataSource<T>(
    private val loadingStates: PublishProcessor<LoadingState>
) : PositionalDataSource<T>() {

    private var lastExecutedRequest: (() -> Boolean)? = null
    private fun storeAndExecute(function: () -> Boolean) {
        function.also { lastExecutedRequest = it }.invoke()
    }

    private fun performWithTryCatch(function: () -> Unit) = try {
        function.invoke()
        loadingStates.offer(LoadingState.Complete)
    } catch (e: Exception) {
        Timber.e(e)
        loadingStates.offer(LoadingState.Error)
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<T>
    ) {
        storeAndExecute {
            loadingStates.offer(LoadingState.InitialLoad)
            performWithTryCatch {
                callback.onResult(
                    getItems(params.requestedLoadSize, params.requestedStartPosition),
                    params.requestedStartPosition
                )
            }
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        storeAndExecute {
            loadingStates.offer(LoadingState.Loading)
            performWithTryCatch {
                callback.onResult(getItems(params.loadSize, params.startPosition))
            }
        }
    }

    protected abstract fun getItems(loadSize: Int, startPosition: Int): List<T>

    fun retryFailedRequest() {
        Completable.fromAction { lastExecutedRequest?.invoke() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }
}

fun <T> dataSource(
    loadingStates: PublishProcessor<LoadingState>,
    loadFunction: LoadFunction<T>
) = object : SearchDataSource<T>(loadingStates) {
    override fun getItems(loadSize: Int, startPosition: Int): List<T> {
        return loadFunction(loadSize, startPosition)
    }
}
