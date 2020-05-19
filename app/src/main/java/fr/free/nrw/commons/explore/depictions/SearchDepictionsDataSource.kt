package fr.free.nrw.commons.explore.depictions

import androidx.paging.PositionalDataSource
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import io.reactivex.Completable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


class SearchDepictionsDataSource constructor(
    private val depictsClient: DepictsClient,
    private val loadingStates: PublishProcessor<LoadingState>,
    val query: String
) : PositionalDataSource<DepictedItem>() {

    var lastExecutedRequest: (() -> Boolean)? = null

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<DepictedItem>
    ) {
        storeAndExecute {
            loadingStates.offer(LoadingState.InitialLoad)
            performWithTryCatch {
                callback.onResult(
                    getItems(query, params.requestedLoadSize, params.requestedStartPosition),
                    params.requestedStartPosition
                )
            }
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<DepictedItem>) {
        storeAndExecute {
            loadingStates.offer(LoadingState.Loading)
            performWithTryCatch {
                callback.onResult(getItems(query, params.loadSize, params.startPosition))
            }
        }
    }

    fun retryFailedRequest() {
        Completable.fromAction { lastExecutedRequest?.invoke() }
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    private fun getItems(query: String, limit: Int, offset: Int) =
        depictsClient.searchForDepictions(query, limit, offset).blockingGet()

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
}
