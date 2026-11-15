package fr.free.nrw.commons.explore.paging

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import fr.free.nrw.commons.databinding.FragmentSearchPaginatedBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment

abstract class BasePagingFragment<T> private constructor(
    private val pagingView: PagingView<T>,
) : CommonsDaggerSupportFragment(),
    PagingContract.View<T> by pagingView {
    /** Retains the no-argument constructor used by paging fragments and Android recreation. */
    constructor() : this(PagingView())

    abstract val pagedListAdapter: PagedListAdapter<T, *>
    abstract val injectedPresenter: PagingContract.Presenter<T>
    abstract val errorTextId: Int
    private val loadingAdapter by lazy { FooterAdapter { injectedPresenter.retryFailedRequest() } }
    private val concatAdapter by lazy { ConcatAdapter(pagedListAdapter, loadingAdapter) }

    protected lateinit var binding: FragmentSearchPaginatedBinding

    init {
        pagingView.bind(this) { binding }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentSearchPaginatedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.paginatedSearchResultsList.apply {
            layoutManager = GridLayoutManager(context, if (isPortrait) 1 else 2)
            adapter = concatAdapter
        }
        injectedPresenter.listFooterData.observe(
            viewLifecycleOwner,
            Observer(loadingAdapter::submitList),
        )
    }

    /**
     * Called on configuration change, update the spanCount according to the orientation state.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.paginatedSearchResultsList.apply {
            layoutManager = GridLayoutManager(context, if (isPortrait) 1 else 2)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        injectedPresenter.onAttachView(this)
    }

    override fun onDetach() {
        super.onDetach()
        injectedPresenter.onDetachView()
    }

    fun onQueryUpdated(query: String) {
        injectedPresenter.onQueryUpdated(query)
    }

    abstract fun getEmptyText(query: String): String
}

private val Fragment.isPortrait get() = orientation == Configuration.ORIENTATION_PORTRAIT

private val Fragment.orientation get() = activity!!.resources.configuration.orientation
