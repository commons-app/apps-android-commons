/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.ui.imagepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.nguyenhoanglam.imagepicker.R
import com.nguyenhoanglam.imagepicker.helper.ImageHelper
import com.nguyenhoanglam.imagepicker.helper.LayoutManagerHelper
import com.nguyenhoanglam.imagepicker.listener.OnFolderClickListener
import com.nguyenhoanglam.imagepicker.model.CallbackStatus
import com.nguyenhoanglam.imagepicker.model.Result
import com.nguyenhoanglam.imagepicker.ui.adapter.FolderPickerAdapter
import com.nguyenhoanglam.imagepicker.widget.GridSpacingItemDecoration
import kotlinx.android.synthetic.main.imagepicker_fragment.*
import kotlinx.android.synthetic.main.imagepicker_fragment.view.*

class FolderFragment : BaseFragment() {

    private var viewModel: ImagePickerViewModel? = null
    private lateinit var folderAdapter: FolderPickerAdapter
    private lateinit var gridLayoutManager: GridLayoutManager
    private lateinit var itemDecoration: GridSpacingItemDecoration

    companion object {
        fun newInstance(): FolderFragment {
            return FolderFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this, ImagePickerViewModelFactory(activity!!.application)).get(ImagePickerViewModel::class.java)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.imagepicker_fragment, container, false)
        root.setBackgroundColor(viewModel!!.getConfig()
            .getBackgroundColor())
        folderAdapter = FolderPickerAdapter(activity!!, activity as OnFolderClickListener)
        gridLayoutManager = LayoutManagerHelper.newInstance(context!!, true)
        itemDecoration = GridSpacingItemDecoration(gridLayoutManager.spanCount, gridLayoutManager.spanCount, false)
        with(root.recyclerView) {
            this.layoutManager = gridLayoutManager
            setHasFixedSize(true)
            addItemDecoration(itemDecoration)
            this.adapter = folderAdapter
        }
        viewModel?.result?.observe(viewLifecycleOwner, Observer {
            handleResult(it)
        })

        return root
    }


    private fun handleResult(result: Result) {
        if (result.status is CallbackStatus.SUCCESS && result.images.isNotEmpty()) {
            val folders = ImageHelper.folderListFromImages(result.images)
            folderAdapter.setData(folders)
            recyclerView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.GONE
        }
        emptyText.visibility = if (result.status is CallbackStatus.SUCCESS && result.images.isEmpty()) View.VISIBLE else View.GONE
        progressWheel.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
    }

    override fun handleOnConfigurationChanged() {
        val newSpanCount = LayoutManagerHelper.getSpanCountForCurrentConfiguration(context!!, true)
        recyclerView.removeItemDecoration(itemDecoration)
        itemDecoration = GridSpacingItemDecoration(newSpanCount, newSpanCount, false)
        gridLayoutManager.spanCount = newSpanCount
        recyclerView.addItemDecoration(itemDecoration)
    }
}