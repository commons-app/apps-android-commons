package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.adapter.ImageAdapter
import kotlinx.android.synthetic.main.fragment_custom_selector.*
import kotlinx.android.synthetic.main.fragment_custom_selector.view.*

class ImageFragment: Fragment() {

    private var bucketId: Long? = null
    private lateinit var  viewModel: CustomSelectorViewModel
    private lateinit var imageAdapter: ImageAdapter
    private lateinit var gridLayoutManager: GridLayoutManager


    companion object {

        const val BUCKET_ID = "BucketId"

        fun newInstance(bucketId: Long): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putLong(BUCKET_ID, bucketId)
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bucketId = arguments?.getLong(BUCKET_ID)
        viewModel = activity!!.run{
            ViewModelProvider(this,CustomSelectorViewModelFactory(activity!!.application)).get(
                CustomSelectorViewModel::class.java
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_custom_selector, container,false)
        imageAdapter = ImageAdapter(activity!!, activity as ImageSelectListener)
        gridLayoutManager = GridLayoutManager(context,getSpanCount())
        with(root.selector_rv){
            this.layoutManager = gridLayoutManager
            setHasFixedSize(true)
            this.adapter = imageAdapter
        }

        viewModel.result.observe(viewLifecycleOwner, Observer{
            handleResult(it)
        })

        //todo selected image observer

        return root
    }

    private fun handleResult(result:Result){
        if(result.status is CallbackStatus.SUCCESS){
            val images= result.images
            if(images.isNotEmpty()){
                imageAdapter.init(images)
                selector_rv.visibility= View.VISIBLE
            }
            else{
                selector_rv.visibility = View.GONE
            }
        }
        loader.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
    }

    private fun getSpanCount(): Int{
        return 3
    }



}