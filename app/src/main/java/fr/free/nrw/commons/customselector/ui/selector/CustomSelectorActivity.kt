package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.theme.BaseActivity
import java.io.File
import javax.inject.Inject

class CustomSelectorActivity: BaseActivity(), FolderClickListener, ImageSelectListener, FragmentManager.OnBackStackChangedListener {

    /**
     * View model.
     */
    private lateinit var viewModel: CustomSelectorViewModel

    /**
     * isImageFragmentOpen is true when the image fragment is in view.
     */
    private var isImageFragmentOpen = false

    /**
     * Current ImageFragment attributes.
     */
    private var bucketId: Long = 0L
    private lateinit var bucketName: String

    /**
     * Pref for saving selector state.
     */
    private lateinit var prefs: SharedPreferences

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

        prefs =  applicationContext.getSharedPreferences("CustomSelector", MODE_PRIVATE)
        viewModel = ViewModelProvider(this, customSelectorViewModelFactory).get(CustomSelectorViewModel::class.java)

        setupViews()

        // Open folder if saved in prefs.
        if(prefs.contains("FolderId")){
            val lastOpenFolderId: Long = prefs.getLong("FolderId", 0L)
            val lastOpenFolderName: String? = prefs.getString("FolderName", null)
            lastOpenFolderName?.let { onFolderClick(lastOpenFolderId, it) }
        }
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
    private fun changeTitle(title: String) {
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
    override fun onFolderClick(folderId: Long, folderName: String) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, ImageFragment.newInstance(folderId))
            .addToBackStack(null)
            .commit()

        changeTitle(folderName)

        bucketId = folderId
        bucketName = folderName
        isImageFragmentOpen = true
    }

    /**
     * override Selected Images Change, update view model selected images.
     */
    override fun onSelectedImagesChanged(selectedImages: ArrayList<Image>) {
        viewModel.selectedImages.value = selectedImages

        val done : ImageButton = findViewById(R.id.done)
        done.visibility = if (selectedImages.isEmpty()) View.INVISIBLE else View.VISIBLE
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

    override fun onDestroy() {
        if(isImageFragmentOpen){
            prefs.edit().putLong("FolderId", bucketId).putString("FolderName", bucketName).apply()
        } else {
            prefs.edit().remove("FolderId").remove("FolderName").apply()
        }
        super.onDestroy()
    }

    /**
     * Called whenever the contents of the back stack change.
     */
    override fun onBackStackChanged() {
        if(supportFragmentManager.backStackEntryCount == 0) {
            isImageFragmentOpen = false
        }
    }
}