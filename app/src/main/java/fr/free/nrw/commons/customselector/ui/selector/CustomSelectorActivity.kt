package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image

class CustomSelectorActivity :AppCompatActivity() , FolderClickListener, ImageSelectListener {

    private lateinit var viewModel: CustomSelectorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_selector)

        viewModel = ViewModelProvider(this, CustomSelectorViewModelFactory(this.application)).get(
            CustomSelectorViewModel::class.java
        )
        setupViews()
    }

    private fun setupViews() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderFragment.newInstance())
            .commit()
        viewModel.fetchImages()
    }

    override fun onFolderClick(folder: Folder) {

    }

    override fun onSelectedImagesChanged(selectedImages: ArrayList<Image>){
    }


}