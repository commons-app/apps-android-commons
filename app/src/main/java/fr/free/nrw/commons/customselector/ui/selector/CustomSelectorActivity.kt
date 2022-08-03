package fr.free.nrw.commons.customselector.ui.selector

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.media.ZoomableActivity
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.android.synthetic.main.custom_selector_bottom_layout.*
import kotlinx.coroutines.*
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
     * NotForUploadStatus Dao class for database operations
     */
    @Inject
    lateinit var notForUploadStatusDao: NotForUploadStatusDao

    /**
     * FileUtilsWrapper class to get imageSHA1 from uri
     */
    @Inject
    lateinit var fileUtilsWrapper: FileUtilsWrapper

    /**
     * Coroutine Dispatchers and Scope.
     */
    private val scope : CoroutineScope = MainScope()
    private var ioDispatcher : CoroutineDispatcher = Dispatchers.IO

    /**
     * Image Fragment instance
     */
    var imageFragment: ImageFragment? = null

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
        if(prefs.contains(FOLDER_ID)){
            val lastOpenFolderId: Long = prefs.getLong(FOLDER_ID, 0L)
            val lastOpenFolderName: String? = prefs.getString(FOLDER_NAME, null)
            val lastItemId: Long = prefs.getLong(ITEM_ID, 0)
            lastOpenFolderName?.let { onFolderClick(lastOpenFolderId, it, lastItemId) }
        }
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
        setUpBottomLayout()
    }

    /**
     * Set up bottom layout
     */
    private fun setUpBottomLayout() {
        val done : Button = findViewById(R.id.upload)
        done.setOnClickListener { onDone() }

        val notForUpload : Button = findViewById(R.id.not_for_upload)
        notForUpload.setOnClickListener{ onClickNotForUpload() }
    }

    /**
     * Gets selected images and proceed for database operations
     */
    private fun onClickNotForUpload() {
        val selectedImages = viewModel.selectedImages.value
        if(selectedImages.isNullOrEmpty()) {
            markAsNotForUpload(arrayListOf())
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
        markAsNotForUpload(selectedImages)
    }

    /**
     * Insert selected images in the database
     */
    private fun markAsNotForUpload(images: ArrayList<Image>) {
        insertIntoNotForUpload(images)
    }

    /**
     * Initializing ImageFragment
     */
    fun setOnDataListener(imageFragment: ImageFragment?) {
        this.imageFragment = imageFragment
    }

    /**
     * Insert images into not for upload
     * Remove images from not for upload
     * Refresh the UI
     */
    private fun insertIntoNotForUpload(images: ArrayList<Image>) {
        scope.launch {
            var allImagesAlreadyNotForUpload = true
            images.forEach{
                val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                    it.uri,
                    ioDispatcher,
                    fileUtilsWrapper,
                    contentResolver
                )
                val exists = notForUploadStatusDao.find(imageSHA1)
                if (exists < 1) {
                    allImagesAlreadyNotForUpload = false
                }
            }

            if (!allImagesAlreadyNotForUpload) {
                images.forEach {
                    val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                        it.uri,
                        ioDispatcher,
                        fileUtilsWrapper,
                        contentResolver
                    )
                    notForUploadStatusDao.insert(
                        NotForUploadStatus(
                            imageSHA1,
                            true
                        )
                    )
                }
            } else {
                images.forEach {
                    val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                        it.uri,
                        ioDispatcher,
                        fileUtilsWrapper,
                        contentResolver
                    )
                    notForUploadStatusDao.deleteNotForUploadWithImageSHA1(imageSHA1)
                }
            }

            imageFragment!!.refresh()
            val bottomLayout : ConstraintLayout = findViewById(R.id.bottom_layout)
            bottomLayout.visibility = View.GONE
        }
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
     * override Selected Images Change, update view model selected images and change UI.
     */
    override fun onSelectedImagesChanged(selectedImages: ArrayList<Image>,
                                         selectedNotForUploadImages: Int) {
        viewModel.selectedImages.value = selectedImages

        if (selectedNotForUploadImages > 0) {
            upload.isEnabled = false
            upload.alpha = 0.5f
        } else {
            upload.isEnabled = true
            upload.alpha = 1f
        }

        not_for_upload.text = when (selectedImages.size == selectedNotForUploadImages) {
                true -> getString(R.string.unmark_as_not_for_upload)
                else -> getString(R.string.mark_as_not_for_upload)
        }

        val bottomLayout : ConstraintLayout = findViewById(R.id.bottom_layout)
        bottomLayout.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * onLongPress
     * @param imageUri : uri of image
     */
    override fun onLongPress(position: Int, images: ArrayList<Image>) {
        val intent = Intent(this, ZoomableActivity::class.java)
        intent.putExtra("b", position);
        intent.putParcelableArrayListExtra("a", images)
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