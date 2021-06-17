package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.theme.BaseActivity
import java.io.File
import javax.inject.Inject

class CustomSelectorActivity : BaseActivity(), FolderClickListener, ImageSelectListener {

    /**
     * View model.
     */
     private lateinit var viewModel: CustomSelectorViewModel

    /**
     * View Model Factory.
     */
    @Inject lateinit var customSelectorViewModelFactory: CustomSelectorViewModelFactory

    /**
     * onCreate Activity, sets theme, initialises the view model, setup view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_selector)

        viewModel = ViewModelProvider(this,customSelectorViewModelFactory).get(CustomSelectorViewModel::class.java)

        setupViews()
    }

    /**
     * Set up view, default folder view.
     */
    private fun setupViews() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderFragment.newInstance())
            .commit()
        fetchData()
        setUpToolbar()

        // todo : open image fragment depending on the last user visit.
    }

    /**
     * Start data fetch in view model.
     */
    private fun fetchData() {
        viewModel.fetchImages()
    }

    /**
     * Change the title of the toolbar.
     */
    private fun changeTitle(title:String) {
        val titleText =  findViewById<TextView>(R.id.title)
        if(titleText != null) {
            titleText.text = title
        }
    }

    /**
     * Set up the toolbar, back listener, done listener.
     */
    private fun setUpToolbar() {
        val back : ImageButton = findViewById(R.id.back)
        back.setOnClickListener { onBackPressed() }

        val done : ImageButton = findViewById(R.id.done)
        done.setOnClickListener { onDone() }
    }

    /**
     * override on folder click, change the toolbar title on folder click.
     */
    override fun onFolderClick(folder: Folder) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, ImageFragment.newInstance(folder.bucketId))
            .addToBackStack(null)
            .commit()
        changeTitle(folder.name)
    }

    /**
     * override Selected Images Change, update view model selected images.
     */
    override fun onSelectedImagesChanged(selectedImages: ArrayList<Image>) {
        viewModel.selectedImages.value = selectedImages
        // todo update selected images in view model.
    }

    /**
     * OnDone clicked.
     * Get the selected images. Remove any non existent file, forward the data to finish selector.
     */
    fun onDone() {
        val selectedImages = viewModel.selectedImages.value
        if(selectedImages.isNullOrEmpty()) {
           finishPickImages(arrayListOf())
            return
        }
        var i = 0
        while (i < selectedImages.size) {
            val path = selectedImages[i].path
            val file = File(path)
            if (!file.exists()) {
                selectedImages.removeAt(i)
                i--
            }
            i++
        }
        finishPickImages(selectedImages)
    }

    /**
     * finishPickImages, Load the data to the intent and set result.
     * Finish the activity.
     */
    private fun finishPickImages(images: ArrayList<Image>) {
        val data = Intent()
        data.putParcelableArrayListExtra("Images", images)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    /**
     * Back pressed.
     * Change toolbar title.
     */
    override fun onBackPressed() {
        super.onBackPressed()
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if(fragment != null && fragment is FolderFragment){
            changeTitle(getString(R.string.custom_selector_title))
        }
    }

}