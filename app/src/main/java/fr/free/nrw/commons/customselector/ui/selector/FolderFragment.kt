package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.ui.adapter.FolderAdapter
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import kotlinx.android.synthetic.main.fragment_custom_selector.*
import kotlinx.android.synthetic.main.fragment_custom_selector.view.*
import javax.inject.Inject

class FolderFragment : CommonsDaggerSupportFragment() {

    /**
     * View Model for images.
     */
    private var viewModel: CustomSelectorViewModel? = null

    /**
     * View Model Factory.
     */
    var customSelectorViewModelFactory: CustomSelectorViewModelFactory? = null
        @Inject set


    /**
     * Folder Adapter.
     */
    private lateinit var folderAdapter: FolderAdapter

    /**
     * Grid Layout Manager for recycler view.
     */
    private lateinit var gridLayoutManager: GridLayoutManager

    /**
     * Companion newInstance.
     */
    companion object{
        fun newInstance(): FolderFragment {
            return FolderFragment()
        }
    }

    /**
     * OnCreate Fragment, get the view model.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(),customSelectorViewModelFactory!!).get(CustomSelectorViewModel::class.java)

    }

    /**
     * OnCreateView.
     * Inflate Layout, init adapter, init gridLayoutManager, setUp recycler view, observe the view model for result.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_custom_selector, container, false)
        folderAdapter = FolderAdapter(activity!!, activity as FolderClickListener)
        gridLayoutManager = GridLayoutManager(context, columnCount())
        with(root.selector_rv){
            this.layoutManager = gridLayoutManager
            setHasFixedSize(true)
            this.adapter = folderAdapter
        }
        viewModel?.result?.observe(viewLifecycleOwner, Observer {
            handleResult(it)
        })
        return root
    }

    /**
     * Handle view model result.
     * Get folders from images.
     * Load adapter.
     */
    private fun handleResult(result: Result) {
        if(result.status is CallbackStatus.SUCCESS){
            val folders = ImageHelper.folderListFromImages(result.images)
            folderAdapter.init(folders)
            folderAdapter.notifyDataSetChanged()
            selector_rv.visibility = View.VISIBLE
        }
        loader.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
    }

    /**
     * Return Column count ie span count for grid view adapter.
     */
    private fun columnCount(): Int {
        return 2
        // todo change column count depending on the orientation of the device.
    }
}