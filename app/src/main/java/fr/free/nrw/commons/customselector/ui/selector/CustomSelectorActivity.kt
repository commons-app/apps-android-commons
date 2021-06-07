package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image

class CustomSelectorActivity : AppCompatActivity(), FolderClickListener, ImageSelectListener {

    /**
     * View model.
     */
    private lateinit var viewModel: CustomSelectorViewModel

    /**
     * onCreate Activity, sets theme, initialises the view model, setup view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getBooleanExtra("DarkTheme", false)){
            setTheme(R.style.DarkAppTheme)
        }
        else{
            setTheme(R.style.LightAppTheme)
        }
        setContentView(R.layout.activity_custom_selector)

        viewModel = ViewModelProvider(this, CustomSelectorViewModelFactory(this.application)).get(
            CustomSelectorViewModel::class.java
        )
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

        // todo done listener.
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
        // todo update selected images in view model.
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


    /**
     *
     * TODO
     * Permission check.
     * OnDone
     * Activity Result.
     *
     *
     */


}