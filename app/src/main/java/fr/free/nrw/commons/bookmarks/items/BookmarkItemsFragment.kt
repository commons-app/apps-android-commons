package fr.free.nrw.commons.bookmarks.items

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.DaggerFragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentBookmarksItemsBinding
import javax.inject.Inject

/**
 * Tab fragment to show list of bookmarked Wikidata Items
 */
class BookmarkItemsFragment : DaggerFragment() {
    private var binding: FragmentBookmarksItemsBinding? = null

    @JvmField
    @Inject
    var controller: BookmarkItemsController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookmarksItemsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initList(requireContext())
    }

    override fun onResume() {
        super.onResume()
        initList(requireContext())
    }

    /**
     * Get list of DepictedItem and sets to the adapter
     * @param context context
     */
    private fun initList(context: Context) {
        val depictItems = controller!!.loadFavoritesItems()
        binding!!.listView.adapter = BookmarkItemsAdapter(depictItems, context)
        binding!!.loadingImagesProgressBar.visibility = View.GONE
        if (depictItems.isEmpty()) {
            binding!!.statusMessage.setText(R.string.bookmark_empty)
            binding!!.statusMessage.visibility = View.VISIBLE
        } else {
            binding!!.statusMessage.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
