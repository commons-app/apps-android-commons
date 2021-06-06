package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.category.GridViewAdapter
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.ui.adapter.FolderAdapter
import kotlinx.android.synthetic.main.fragment_custom_selector.*
import kotlinx.android.synthetic.main.fragment_custom_selector.view.*

class FolderFragment : Fragment() {

    private var viewModel: CustomSelectorViewModel? = null
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var gridLayoutManager: GridLayoutManager

    companion object{
        fun newInstance(): FolderFragment {
            return FolderFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run { ViewModelProvider(this,CustomSelectorViewModelFactory(activity!!.application)).get(CustomSelectorViewModel::class.java) }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root=inflater.inflate(R.layout.fragment_custom_selector,container,false)
        folderAdapter = FolderAdapter(activity!!, activity as FolderClickListener)
        gridLayoutManager = GridLayoutManager(context, columnCount())
        with(root.selector_rv){
            this.layoutManager= gridLayoutManager
            setHasFixedSize(true)
            this.adapter = folderAdapter
        }
        viewModel?.result?.observe(viewLifecycleOwner, Observer {
            setupView(it)
        })
        return root
    }

    private fun setupView(result: Result){
        if(result.status is CallbackStatus.SUCCESS){
            val folders = arrayListOf<Folder>()
            for( i in 1..12){
                folders.add(Folder(i.toLong(), "Folder$i",result.images))
            }
            folderAdapter.init(folders)
            folderAdapter.notifyDataSetChanged()
            selector_rv.visibility=View.VISIBLE
        }
        loader.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
    }
    private fun columnCount():Int {
        return 2
    }
}