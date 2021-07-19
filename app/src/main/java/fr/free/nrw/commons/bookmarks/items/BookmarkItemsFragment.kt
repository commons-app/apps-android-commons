package fr.free.nrw.commons.bookmarks.items

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import dagger.android.support.DaggerFragment
import fr.free.nrw.commons.R
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject


class BookmarkItemsFragment : DaggerFragment() {

    @BindView(R.id.status_message)
    lateinit var statusTextView: TextView

    @BindView(R.id.loading_images_progress_bar)
    lateinit var progressBar: ProgressBar

    @BindView(R.id.list_view)
    lateinit var recyclerView: RecyclerView

    @BindView(R.id.parent_layout)
    lateinit var parentLayout: RelativeLayout

    @Inject
    lateinit var controller: BookmarkItemsController

    private lateinit var adapter: BookmarkItemsAdapter

    fun newInstance(): BookmarkItemsFragment {
        return BookmarkItemsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_bookmarks_items, container, false)
        ButterKnife.bind(this, v)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar.visibility = View.VISIBLE
        recyclerView.layoutManager = LinearLayoutManager(context)
        initList(requireContext())
    }

    override fun onResume() {
        super.onResume()
        initList(requireContext())
    }

    private fun initList(context: Context) {
        val depictItems: List<DepictedItem> = controller.loadFavoritesItems()
        adapter = BookmarkItemsAdapter(depictItems, context)
        recyclerView.adapter = adapter
        progressBar.visibility = View.GONE
        if (depictItems.isEmpty()) {
            statusTextView.setText(R.string.bookmark_empty)
            statusTextView.visibility = View.VISIBLE
        } else {
            statusTextView.visibility = View.GONE
        }
    }
}