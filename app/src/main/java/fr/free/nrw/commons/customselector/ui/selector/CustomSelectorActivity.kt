package fr.free.nrw.commons.customselector.ui.selector

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewGroupCompat
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.MAX_IMAGE_COUNT
import fr.free.nrw.commons.customselector.helper.NotForUploadMarker
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.databinding.ActivityCustomSelectorBinding
import fr.free.nrw.commons.databinding.CustomSelectorBottomLayoutBinding
import fr.free.nrw.commons.databinding.CustomSelectorToolbarBinding
import fr.free.nrw.commons.media.ZoomableActivity
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import fr.free.nrw.commons.utils.applyEdgeToEdgeBottomPaddingInsets
import fr.free.nrw.commons.utils.applyEdgeToEdgeTopInsets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Integer.max
import javax.inject.Inject

/**
 * Custom Selector Activity.
 */
class CustomSelectorActivity :
    BaseActivity(),
    FolderClickListener,
    ImageSelectListener {
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
     * Keeps track of the opened folder, the toolbar and the deletion of the opened folder.
     */
    private val folderBrowser: FolderBrowser by lazy {
        FolderBrowser(
            activity = this,
            viewModel = viewModel,
            defaultKvStore = defaultKvStore,
            folderDeletionLauncher = startForFolderDeletionResult,
            onUploadLimitErrorClick = {
                CustomSelectorDialogs.showUploadLimitWarning(this, uploadLimit, uploadLimitExceededBy)
            },
        )
    }

    /**
     * Pref for saving selector state.
     */
    private lateinit var prefs: SharedPreferences

    /**
     * Maximum number of images that can be selected.
     */
    private var uploadLimit: Int = MAX_IMAGE_COUNT

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
     * Marks and unmarks the selected images as not for upload.
     */
    private val notForUploadMarker: NotForUploadMarker by lazy {
        NotForUploadMarker(notForUploadStatusDao, fileUtilsWrapper, contentResolver, ioDispatcher)
    }

    /**
     * Image Fragment instance
     */
    var imageFragment: ImageFragment? = null

    private var progressDialogText: String = ""

    private var showPartialAccessIndicator by mutableStateOf(false)

    /**
     * Waits for confirmation of delete folder
     */
    private val startForFolderDeletionResult =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            folderBrowser.onDeleteFolderResultReceived(result)
        }

    private val startForResult =
        registerForActivityResult(StartActivityForResult()) { result ->
            viewModel.onFullScreenDataReceived(result)
        }

    /**
     * onCreate Activity, sets theme, initialises the view model, setup view.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_DENIED
        ) {
            showPartialAccessIndicator = true
        }

        binding = ActivityCustomSelectorBinding.inflate(layoutInflater)
        toolbarBinding = CustomSelectorToolbarBinding.bind(binding.root)
        bottomSheetBinding = CustomSelectorBottomLayoutBinding.bind(binding.root)
        binding.partialAccessIndicator.setContent {
            PartialStorageAccessIndicator(
                isVisible = showPartialAccessIndicator,
                onManage = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 1)
                    }
                },
                modifier =
                    Modifier
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .fillMaxWidth(),
            )
        }
        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        applyEdgeToEdgeTopInsets(toolbarBinding.toolbarLayout)
        bottomSheetBinding.bottomLayout.applyEdgeToEdgeBottomPaddingInsets()
        val view = binding.root
        setContentView(view)

        prefs = applicationContext.getSharedPreferences("CustomSelector", MODE_PRIVATE)
        viewModel =
            ViewModelProvider(this, customSelectorViewModelFactory).get(
                CustomSelectorViewModel::class.java,
            )

        // Check for single selection extra
        uploadLimit = if (intent.getBooleanExtra(EXTRA_SINGLE_SELECTION, false)) 1 else MAX_IMAGE_COUNT

        folderBrowser.setUpViews()

        // Set up bottom layout
        findViewById<Button>(R.id.upload).setOnClickListener { onDone() }
        findViewById<Button>(R.id.not_for_upload).setOnClickListener { onClickNotForUpload() }

        if (prefs.getBoolean("customSelectorFirstLaunch", true)) {
            // show welcome dialog on first launch
            CustomSelectorDialogs.showWelcomeDialog(this)
            prefs.edit().putBoolean("customSelectorFirstLaunch", false).apply()
        }

        // Open folder if saved in prefs.
        if (prefs.contains(FOLDER_ID)) {
            val lastOpenFolderId: Long = prefs.getLong(FOLDER_ID, 0L)
            val lastOpenFolderName: String? = prefs.getString(FOLDER_NAME, null)
            val lastItemId: Long = prefs.getLong(ITEM_ID, 0)
            lastOpenFolderName?.let { onFolderClick(lastOpenFolderId, it, lastItemId) }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                    folderBrowser.onNavigatedBack()
                }
            },
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPartialAccessIndicator = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Start data fetch in view model.
        viewModel.fetchImages()
    }

    /**
     * Gets selected images and proceed for database operations
     */
    private fun onClickNotForUpload() {
        val selectedImages = viewModel.selectedImages.value ?: arrayListOf()
        val hasSelectedImages = selectedImages.isNotEmpty()
        selectedImages.removeAll { !File(it.path).exists() }

        // Insert selected images in the database
        scope.launch {
            withContext(Dispatchers.Main) {
                imageFragment?.showMarkUnmarkProgressDialog(text = progressDialogText)
            }

            // Insert or delete images as necessary, but the UI updates should be posted back to the main thread
            val marked = notForUploadMarker.toggle(selectedImages)
            withContext(Dispatchers.Main) {
                if (marked) {
                    selectedImages.forEach { image ->
                        imageFragment?.removeImage(image)
                    }
                    imageFragment?.clearSelectedImages()
                } else {
                    // Refresh the UI
                    imageFragment?.refresh()
                }
            }

            withContext(Dispatchers.Main) {
                imageFragment?.dismissMarkUnmarkProgressDialog()
                val bottomLayout: ConstraintLayout = findViewById(R.id.bottom_layout)
                bottomLayout.visibility = View.GONE
                folderBrowser.changeTitle(folderBrowser.bucketName, 0)
            }
        }

        if (hasSelectedImages) {
            toolbarBinding.imageLimitError.visibility = View.INVISIBLE
        }
    }

    /**
     * Initializing ImageFragment
     */
    fun setOnDataListener(imageFragment: ImageFragment?) {
        this.imageFragment = imageFragment
    }

    /**
     * override on folder click,
     * change the toolbar title on folder click, make overflow menu visible
     */
    override fun onFolderClick(
        folderId: Long,
        folderName: String,
        lastItemId: Long,
    ) {
        folderBrowser.openFolder(folderId, folderName, lastItemId)
    }

    /**
     * override Selected Images Change, update view model selected images and change UI.
     */
    override fun onSelectedImagesChanged(
        selectedImages: ArrayList<Image>,
        selectedNotForUploadImages: Int,
    ) {
        viewModel.selectedImages.value = selectedImages
        folderBrowser.changeTitle(folderBrowser.bucketName, selectedImages.size)

        uploadLimitExceeded = selectedImages.size > uploadLimit
        uploadLimitExceededBy = max(selectedImages.size - uploadLimit, 0)

        if (uploadLimitExceeded && selectedNotForUploadImages == 0) {
            toolbarBinding.imageLimitError.visibility = View.VISIBLE
            bottomSheetBinding.upload.text =
                resources.getString(
                    R.string.custom_selector_button_limit_text,
                    uploadLimit,
                )
        } else {
            toolbarBinding.imageLimitError.visibility = View.INVISIBLE
            bottomSheetBinding.upload.text = resources.getString(R.string.upload)
        }

        if (uploadLimitExceeded || selectedNotForUploadImages > 0) {
            bottomSheetBinding.upload.isEnabled = false
            bottomSheetBinding.upload.alpha = DISABLED_BUTTON_ALPHA
        } else {
            bottomSheetBinding.upload.isEnabled = true
            bottomSheetBinding.upload.alpha = 1f
        }

        bottomSheetBinding.notForUpload.text =
            when (selectedImages.size == selectedNotForUploadImages) {
                true -> {
                    progressDialogText = getString(R.string.unmarking_as_not_for_upload)
                    getString(R.string.unmark_as_not_for_upload)
                }
                else -> {
                    progressDialogText = getString(R.string.marking_as_not_for_upload)
                    getString(R.string.mark_as_not_for_upload)
                }
            }

        val bottomLayout: ConstraintLayout = findViewById(R.id.bottom_layout)
        bottomLayout.visibility = if (selectedImages.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Triggered when the user performs a long press on an image.
     *
     * @param position The index of the selected image.
     * @param images The list of all available images.
     * @param selectedImages The list of images currently selected.
     */
    override fun onLongPress(
        position: Int,
        images: ArrayList<Image>,
        selectedImages: ArrayList<Image>,
    ) {
        val intent = Intent(this, ZoomableActivity::class.java)
        intent.putExtra(CustomSelectorConstants.PRESENT_POSITION, position)
        intent.putParcelableArrayListExtra(
            CustomSelectorConstants.TOTAL_SELECTED_IMAGES,
            selectedImages,
        )
        intent.putExtra(CustomSelectorConstants.BUCKET_ID, folderBrowser.bucketId)
        startForResult.launch(intent)
    }

    /**
     * OnDone clicked.
     * Get the selected images. Remove any non existent file, forward the data to finish selector.
     */
    fun onDone() {
        /**
         * finishPickImages, Load the data to the intent and set result.
         * Finish the activity.
         */
        fun finishPickImages(images: ArrayList<Image>) {
            val data = Intent()
            data.putParcelableArrayListExtra("Images", images)
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        val selectedImages = viewModel.selectedImages.value
        if (selectedImages.isNullOrEmpty()) {
            finishPickImages(arrayListOf())
            return
        }
        scope.launch(ioDispatcher) {
            val uniqueImages =
                selectedImages.distinctBy { image ->
                    CustomSelectorUtils.getImageSHA1(
                        image.uri,
                        ioDispatcher,
                        fileUtilsWrapper,
                        contentResolver,
                    )
                }

            withContext(Dispatchers.Main) {
                if (uniqueImages.size != selectedImages.size) {
                    CustomSelectorDialogs.showDuplicateSelectionWarning(this@CustomSelectorActivity) {
                        finishPickImages(ArrayList(uniqueImages))
                    }
                } else {
                    finishPickImages(ArrayList(uniqueImages))
                }
            }
        }
    }

    /**
     * On activity destroy
     * If image fragment is open, overwrite its attributes otherwise discard the values.
     */
    override fun onDestroy() {
        if (folderBrowser.isImageFragmentOpen) {
            prefs
                .edit()
                .putLong(FOLDER_ID, folderBrowser.bucketId)
                .putString(FOLDER_NAME, folderBrowser.bucketName)
                .apply()
        } else {
            prefs
                .edit()
                .remove(FOLDER_ID)
                .remove(FOLDER_NAME)
                .apply()
        }
        super.onDestroy()
    }

    companion object {
        const val FOLDER_ID: String = "FolderId"
        const val FOLDER_NAME: String = "FolderName"
        const val ITEM_ID: String = "ItemId"
        const val EXTRA_SINGLE_SELECTION: String = "EXTRA_SINGLE_SELECTION"
        private const val DISABLED_BUTTON_ALPHA = 0.5f
    }
}

@Composable
fun PartialStorageAccessIndicator(
    isVisible: Boolean,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        OutlinedCard(
            modifier = modifier,
            colors =
                CardDefaults.cardColors(
                    containerColor = colorResource(R.color.primarySuperLightColor),
                ),
            border = BorderStroke(0.5.dp, color = colorResource(R.color.primaryColor)),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(
                    text = "You've given access to a select number of photos",
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onManage,
                    modifier = Modifier.align(Alignment.Bottom),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorResource(R.color.primaryColor),
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelMedium,
                        color = colorResource(R.color.primaryTextColor),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PartialStorageAccessIndicatorPreview() {
    Surface {
        PartialStorageAccessIndicator(
            isVisible = true,
            onManage = {},
            modifier =
                Modifier
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .fillMaxWidth(),
        )
    }
}
