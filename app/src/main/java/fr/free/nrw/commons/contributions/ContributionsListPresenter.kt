package fr.free.nrw.commons.contributions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.di.CommonsApplicationModule
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject
import javax.inject.Named


/**
 * The presenter class for Contributions
 */
class ContributionsListPresenter @Inject internal constructor(
    private val contributionBoundaryCallback: ContributionBoundaryCallback,
    private val contributionsRemoteDataSource: ContributionsRemoteDataSource,
    private val repository: ContributionsRepository,
    @param:Named(CommonsApplicationModule.IO_THREAD) private val ioThreadScheduler: Scheduler
) : ContributionsListContract.UserActionListener {
    private val compositeDisposable = CompositeDisposable()

    @Inject
    lateinit var sessionManager: SessionManager

    var contributionList: LiveData<PagedList<Contribution>>? = null

    private val liveData = MutableLiveData<List<Contribution>>()

    private val existingContributions: MutableList<Contribution> = ArrayList()

    override fun onAttachView(view: ContributionsListContract.View) {
    }

    /**
     * Setup the paged list. This method sets the configuration for paged list and ties it up with
     * the live data object. This method can be tweaked to update the lazy loading behavior of the
     * contributions list
     */
    fun setup(userName: String?, isSelf: Boolean) {
        val pagedListConfig =
            (PagedList.Config.Builder())
                .setPrefetchDistance(50)
                .setPageSize(10).build()
        val factory: DataSource.Factory<Int, Contribution>
        val shouldSetBoundaryCallback: Boolean
        if (!isSelf) {
            //We don't want to persist contributions for other user's, therefore
            // creating a new DataSource for them
            contributionsRemoteDataSource.userName = userName
            factory = object : DataSource.Factory<Int, Contribution>() {
                override fun create(): DataSource<Int, Contribution> {
                    return contributionsRemoteDataSource
                }
            }
            shouldSetBoundaryCallback = false
        } else {
            contributionBoundaryCallback.userName = userName
            shouldSetBoundaryCallback = true
            factory = repository.fetchContributionsWithStates(
                listOf(Contribution.STATE_COMPLETED)
            )
        }

        val livePagedListBuilder: LivePagedListBuilder<Int, Contribution> = LivePagedListBuilder(
            factory,
            pagedListConfig
        )
        if (shouldSetBoundaryCallback) {
            livePagedListBuilder.setBoundaryCallback(contributionBoundaryCallback)
        }

        contributionList = livePagedListBuilder.build()
        contributionList!!.observeForever { pagedList ->
            pagedList?.let {
                existingContributions.clear()
                existingContributions.addAll(it)
                liveData.value = existingContributions // Update liveData with the latest list
            }
        }
    }

    override fun onDetachView() {
        compositeDisposable.clear()
        contributionsRemoteDataSource.dispose()
        contributionBoundaryCallback.dispose()
    }

    /**
     * It is used to refresh list.
     *
     * @param swipeRefreshLayout used to stop refresh animation when
     * refresh finishes.
     */
    override fun refreshList(swipeRefreshLayout: SwipeRefreshLayout?) {
        contributionBoundaryCallback.refreshList {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.isRefreshing = false
            }
            Unit
        }
    }
}
