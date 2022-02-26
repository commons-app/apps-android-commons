package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.media.ZoomableActivity
import fr.free.nrw.commons.theme.BaseActivity
import java.io.File
import javax.inject.Inject


/**
 * Custom Selector Activity.
 */
class CustomSelectorActivity: BaseActivity(), FolderClickListener, ImageSelectListener {

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
        viewModel = ViewModelProvider(this, customSelectorViewModelFactory).get(
            CustomSelectorViewModel::class.java
        )

        setupViews()

        if(prefs.getBoolean("customSelectorFirstLaunch", true)) {
            // show welcome dialog on first launch
            showWelcomeDialog()
            prefs.edit().putBoolean("customSelectorFirstLaunch", false).apply()
        }

        // Open folder if saved in prefs.
        if(prefs.contains(FOLDER_ID)) {
            val lastOpenFolderId: Long = prefs.getLong(FOLDER_ID, 0L)
            val lastOpenFolderName: String? = prefs.getString(FOLDER_NAME, null)
            val lastItemId: Long = prefs.getLong(ITEM_ID, 0)
            lastOpenFolderName?.let { onFolderClick(lastOpenFolderId, it, lastItemId) }
        }

        // Checks if the Image gallery is empty.
            if(isImageGalleryEmpty())
        {
            val linearLayout: LinearLayout = findViewById<View>(R.id.linear_layout_noimage) as LinearLayout
            val textView = TextView(this)
            textView.text = "No Image Found"
            textView.textSize = 20f
            linearLayout.addView(textView)
        }
    }

    /**
     * Checks for no image in gallery.
     */
        private fun isImageGalleryEmpty(): Boolean {
        try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val cursor: Cursor? = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null
            )

            val size: Int? = cursor?.count
            // If size is 0, there are no images on the SD Card.
            return size == 0

        } catch (e: Exception) {
        }
        return false
    }

    /**
     * Show Custom Selector Welcome Dialog.
     */
    private fun showWelcomeDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_selector_info_dialog)
        (dialog.findViewById(R.id.btn_ok) as Button).setOnClickListener { dialog.dismiss() }
        dialog.show()
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
    override fun onFolderClick(folderId: Long, folderName: String, lastItemId: Long) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, ImageFragment.newInstance(folderId, lastItemId))
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
     * onLongPress
     * @param imageUri : uri of image
     */
    override fun onLongPress(imageUri: Uri) {
        val intent = Intent(this, ZoomableActivity::class.java).setData(imageUri);
        startActivity(intent)
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
            isImageFragmentOpen = false
            changeTitle(getString(R.string.custom_selector_title))
        }
    }

    /**
     * On activity destroy
     * If image fragment is open, overwrite its attributes otherwise discard the values.
     */
    override fun onDestroy() {
        if(isImageFragmentOpen){
            prefs.edit().putLong(FOLDER_ID, bucketId).putString(FOLDER_NAME, bucketName).apply()
        } else {
            prefs.edit().remove(FOLDER_ID).remove(FOLDER_NAME).apply()
        }
        super.onDestroy()
    }

    companion object {
        const val FOLDER_ID : String = "FolderId"
        const val FOLDER_NAME : String = "FolderName"
        const val ITEM_ID : String = "ItemId"
    }
}