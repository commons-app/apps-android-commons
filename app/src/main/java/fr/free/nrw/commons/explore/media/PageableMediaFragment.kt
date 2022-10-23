package fr.free.nrw.commons.explore.media

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.category.CategoryImagesCallback
import fr.free.nrw.commons.databinding.FragmentSearchPaginatedBinding
import fr.free.nrw.commons.explore.paging.BasePagingFragment
import fr.free.nrw.commons.media.MediaDetailPagerFragment.MediaDetailProvider

abstract class PageableMediaFragment : BasePagingFragment<Media>(), MediaDetailProvider {

    /**
     * ViewBinding
     */
    private var _binding: FragmentSearchPaginatedBinding? = null
    private val binding get() = _binding

    override val pagedListAdapter by lazy {
        PagedMediaAdapter(categoryImagesCallback::onMediaClicked)
    }

    override val errorTextId: Int = R.string.error_loading_images

    override fun getEmptyText(query: String) = getString(R.string.no_images_found)

    lateinit var categoryImagesCallback: CategoryImagesCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment != null) {
            categoryImagesCallback = (parentFragment as CategoryImagesCallback)
        } else {
            categoryImagesCallback = (activity as CategoryImagesCallback)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchPaginatedBinding.inflate(inflater, container, false)
        return binding?.root
    }

    private val simpleDataObserver =
        SimpleDataObserver { categoryImagesCallback.viewPagerNotifyDataSetChanged() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pagedListAdapter.registerAdapterDataObserver(simpleDataObserver)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        pagedListAdapter.unregisterAdapterDataObserver(simpleDataObserver)
    }

    override fun getMediaAtPosition(position: Int): Media? =
        pagedListAdapter.currentList?.get(position)?.takeIf { it.filename != null }
            .also {
                pagedListAdapter.currentList?.loadAround(position)
                binding?.paginatedSearchResultsList?.scrollToPosition(position)
            }

    override fun getTotalMediaCount(): Int = pagedListAdapter.itemCount

    override fun getContributionStateAt(position: Int) = null

    /**
     * Reload media detail fragment once media is nominated
     *
     * @param index item position that has been nominated
     */
    override fun refreshNominatedMedia(index: Int) {
        activity?.onBackPressed()
        categoryImagesCallback.onMediaClicked(index)
    }
}
