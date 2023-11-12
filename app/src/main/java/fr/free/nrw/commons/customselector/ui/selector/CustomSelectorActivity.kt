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
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.SHOULD_REFRESH
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.databinding.ActivityCustomSelectorBinding
import fr.free.nrw.commons.databinding.CustomSelectorBottomLayoutBinding
import fr.free.nrw.commons.databinding.CustomSelectorToolbarBinding
import fr.free.nrw.commons.filepicker.Constants
import fr.free.nrw.commons.media.ZoomableActivity
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.*
import java.io.File
import java.lang.Integer.max
import javax.inject.Inject


/**
 * Custom Selector Activity.
 */
class CustomSelectorActivity : BaseActivity(), FolderClickListener, ImageSelectListener {

    /**
     * ViewBindings
     */
    private lateinit var binding: ActivityCustomSelectorBinding
    private lateinit var toolbarBinding: CustomSelectorToolbarBinding
    private lateinit var bottomSheetBinding: CustomSelectorBottomLayoutBinding

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
     * Maximum number of images that can be selected.
     */
    private val uploadLimit: Int = 20

    /**
     * Flag that is marked true when the amount
     * of selected images is greater than the upload limit.
     */
    private var uploadLimitExceeded: Boolean = false

    /**
     * Tracks the amount by which the upload limit has been exceeded.
     */
    private var uploadLimitExceededBy: Int = 0

    /**
     * View Model Factory.
     */
    @Inject
    lateinit var customSelectorViewModelFactory: CustomSelectorViewModelFactory

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
    private val scope: CoroutineScope = MainScope()
    private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Image Fragment instance
     */
    var imageFragment: ImageFragment? = null

    private var progressDialogText:String=""

    /**
     * onCreate Activity, sets theme, initialises the view model, setup view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomSelectorBinding.inflate(layoutInflater)
        toolbarBinding = CustomSelectorToolbarBinding.bind(binding.root)
        bottomSheetBinding = CustomSelectorBottomLayoutBinding.bind(binding.root)
        val view = binding.root
        setContentView(view)

        prefs = applicationContext.getSharedPreferences("CustomSelector", MODE_PRIVATE)
        viewModel = ViewModelProvider(this, customSelectorViewModelFactory).get(
            CustomSelectorViewModel::class.java
        )

        setupViews()

        if (prefs.getBoolean("customSelectorFirstLaunch", true)) {
            // show welcome dialog on first launch
            showWelcomeDialog()
            prefs.edit().putBoolean("customSelectorFirstLaunch", false).apply()
        }

        // Open folder if saved in prefs.
        if (prefs.contains(FOLDER_ID)) {
            val lastOpenFolderId: Long = prefs.getLong(FOLDER_ID, 0L)
            val lastOpenFolderName: String? = prefs.getString(FOLDER_NAME, null)
            val lastItemId: Long = prefs.getLong(ITEM_ID, 0)
            lastOpenFolderName?.let { onFolderClick(lastOpenFolderId, it, lastItemId) }
        }
    }

    /**
     * When data will be send from full screen mode, it will be passed to fragment
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.RequestCodes.RECEIVE_DATA_FROM_FULL_SCREEN_MODE &&
            resultCode == Activity.RESULT_OK
        ) {
            val selectedImages: ArrayList<Image> =
                data!!
                    .getParcelableArrayListExtra(CustomSelectorConstants.NEW_SELECTED_IMAGES)!!
            val shouldRefresh = data.getBooleanExtra(SHOULD_REFRESH, false)
            imageFragment?.passSelectedImages(selectedImages, shouldRefresh)
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
        val done: Button = findViewById(R.id.upload)
        done.setOnClickListener { onDone() }

        val notForUpload: Button = findViewById(R.id.not_for_upload)
        notForUpload.setOnClickListener { onClickNotForUpload() }
    }

    /**
     * Gets selected images and proceed for database operations
     */
    private fun onClickNotForUpload() {
        val selectedImages = viewModel.selectedImages.value
        if (selectedImages.isNullOrEmpty()) {
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
        toolbarBinding.imageLimitError.visibility = View.INVISIBLE
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
            imageFragment!!.showMarkUnmarkProgressDialog(
                text= progressDialogText
            )

            var allImagesAlreadyNotForUpload = true
            images.forEach {
                val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                    it.uri,
                    ioDispatcher,
                    fileUtilsWrapper,
                    contentResolver
                )
                val exists = notForUploadStatusDao.find(imageSHA1)

                // If image exists in not for upload table make allImagesAlreadyNotForUpload false
                if (exists < 1) {
                    allImagesAlreadyNotForUpload = false
                }
            }

            // if all images is not already marked as not for upload, insert all images in
            // not for upload table
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
                            imageSHA1
                        )
                    )
                }

                // if all images is already marked as not for upload, delete all images from
                // not for upload table
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
            imageFragment!!.dismissMarkUnmarkProgressDialog()

            val bottomLayout: ConstraintLayout = findViewById(R.id.bottom_layout)
            bottomLayout.visibility = View.GONE
        }
        changeTitle(bucketName, 0)
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
    private fun changeTitle(title: String, selectedImageCount:Int) {
        if (title.isNotEmpty()){
            val titleText = findViewById<TextView>(R.id.title)
            var titleWithAppendedImageCount = title
            if (selectedImageCount > 0) {
                titleWithAppendedImageCount += " (${resources.getQuantityString(R.plurals.custom_picker_images_selected_title_appendix, 
                    selectedImageCount, selectedImageCount)})"
            }
            if (titleText != null) {
                titleText.text = titleWithAppendedImageCount
            }
        }
    }

    /**
     * Set up the toolbar, back listener, done listener.
     */
    private fun setUpToolbar() {
        val back: ImageButton = findViewById(R.id.back)
        back.setOnClickListener { onBackPressed() }

        val limitError: ImageButton = findViewById(R.id.image_limit_error)
        limitError.visibility = View.INVISIBLE
        limitError.setOnClickListener { displayUploadLimitWarning() }
    }

    /**
     * override on folder click, change the toolbar title on folder click.
     */
    override fun onFolderClick(folderId: Long, folderName: String, lastItemId: Long) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, ImageFragment.newInstance(folderId, lastItemId))
            .addToBackStack(null)
            .commit()

        changeTitle(folderName, 0)

        bucketId = folderId
        bucketName = folderName
        isImageFragmentOpen = true
    }

    /**
     * override Selected Images Change, update view model selected images and change UI.
     */
    override fun onSelectedImagesChanged(
        selectedImages: ArrayList<Image>,
        selectedNotForUploadImages: Int
    ) {
        viewModel.selectedImages.value = selectedImages
        changeTitle(bucketName, selectedImages.size)

        uploadLimitExceeded = selectedImages.size > uploadLimit
        uploadLimitExceededBy = max(selectedImages.size - uploadLimit,0)

        if (uploadLimitExceeded && selectedNotForUploadImages == 0) {
            toolbarBinding.imageLimitError.visibility = View.VISIBLE
            bottomSheetBinding.upload.text = resources.getString(
                R.string.custom_selector_button_limit_text, uploadLimit)
        } else {
            toolbarBinding.imageLimitError.visibility = View.INVISIBLE
            bottomSheetBinding.upload.text = resources.getString(R.string.upload)
        }

        if (uploadLimitExceeded || selectedNotForUploadImages > 0) {
            bottomSheetBinding.upload.isEnabled = false
            bottomSheetBinding.upload.alpha = 0.5f
        } else {
            bottomSheetBinding.upload.isEnabled = true
            bottomSheetBinding.upload.alpha = 1f
        }

        bottomSheetBinding.notForUpload.text =
            when (selectedImages.size == selectedNotForUploadImages) {
                true -> {
                    progressDialogText=getString(R.string.unmarking_as_not_for_upload)
                    getString(R.string.unmark_as_not_for_upload)
                }
                else -> {
                    progressDialogText=getString(R.string.marking_as_not_for_upload)
                    getString(R.string.mark_as_not_for_upload)
                }
            }

        val bottomLayout: ConstraintLayout = findViewById(R.id.bottom_layout)
        bottomLayout.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * onLongPress
     * @param imageUri : uri of image
     */
    override fun onLongPress(
        position: Int,
        images: ArrayList<Image>,
        selectedImages: ArrayList<Image>
    ) {
        val intent = Intent(this, ZoomableActivity::class.java)
        intent.putExtra(CustomSelectorConstants.PRESENT_POSITION, position)
        intent.putParcelableArrayListExtra(
            CustomSelectorConstants.TOTAL_SELECTED_IMAGES,
            selectedImages
        )
        intent.putExtra(CustomSelectorConstants.BUCKET_ID, bucketId)
        startActivityForResult(intent, Constants.RequestCodes.RECEIVE_DATA_FROM_FULL_SCREEN_MODE)
    }

    /**
     * OnDone clicked.
     * Get the selected images. Remove any non existent file, forward the data to finish selector.
     */
    fun onDone() {
            val selectedImages = viewModel.selectedImages.value
            if (selectedImages.isNullOrEmpty()) {
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
        if (fragment != null && fragment is FolderFragment) {
            isImageFragmentOpen = false
            changeTitle(getString(R.string.custom_selector_title), 0)
        }
    }

    /**
     * Displays a dialog explaining the upload limit warning.
     */
    private fun displayUploadLimitWarning() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_selector_limit_dialog)
        (dialog.findViewById(R.id.btn_dismiss_limit_warning) as Button).setOnClickListener()
        { dialog.dismiss() }
        (dialog.findViewById(R.id.upload_limit_warning) as TextView).text = resources.getString(
            R.string.custom_selector_over_limit_warning, uploadLimit, uploadLimitExceededBy)
        dialog.show()
    }

    /**
     * On activity destroy
     * If image fragment is open, overwrite its attributes otherwise discard the values.
     */
    override fun onDestroy() {
        if (isImageFragmentOpen) {
            prefs.edit().putLong(FOLDER_ID, bucketId).putString(FOLDER_NAME, bucketName).apply()
        } else {
            prefs.edit().remove(FOLDER_ID).remove(FOLDER_NAME).apply()
        }
        super.onDestroy()
    }

    companion object {
        const val FOLDER_ID: String = "FolderId"
        const val FOLDER_NAME: String = "FolderName"
        const val ITEM_ID: String = "ItemId"
    }
}