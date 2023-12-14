package fr.free.nrw.commons.media

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import butterknife.BindView
import butterknife.ButterKnife
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.imagepipeline.image.ImageInfo
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.SHOULD_REFRESH
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.helper.OnSwipeTouchListener
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorViewModel
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorViewModelFactory
import fr.free.nrw.commons.media.zoomControllers.zoomable.DoubleTapGestureListener
import fr.free.nrw.commons.media.zoomControllers.zoomable.ZoomableDraweeView
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * Activity for helping to view an image in full-screen mode with some other features
 * like zoom, and swap gestures
 */
class ZoomableActivity : BaseActivity() {

    private lateinit var imageUri: Uri

    /**
     * View model.
     */
    private lateinit var viewModel: CustomSelectorViewModel

    /**
     * Pref for saving states.
     */
    private lateinit var prefs: SharedPreferences

    @JvmField
    @BindView(R.id.zoomable)
    var photo: ZoomableDraweeView? = null
    
    var photoBackgroundColor: Int? = null

    @JvmField
    @BindView(R.id.zoom_progress_bar)
    var spinner: ProgressBar? = null

    @JvmField
    @BindView(R.id.selection_count)
    var selectedCount: TextView? = null

    /**
     * Total images present in folder
     */
    private var images: ArrayList<Image>? = null

    /**
     * Total selected images present in folder
     */
    private var selectedImages: ArrayList<Image>? = null

    /**
     * Present position of the image
     */
    private var position = 0

    /**
     * Present bucket ID
     */
    private var bucketId: Long = 0L

    /**
     * Determines whether the adapter should refresh
     */
    private var shouldRefresh = false

    /**
     * FileUtilsWrapper class to get imageSHA1 from uri
     */
    @Inject
    lateinit var fileUtilsWrapper: FileUtilsWrapper

    /**
     * FileProcessor to pre-process the file.
     */
    @Inject
    lateinit var fileProcessor: FileProcessor

    /**
     * NotForUploadStatus Dao class for database operations
     */
    @Inject
    lateinit var notForUploadStatusDao: NotForUploadStatusDao

    /**
     * UploadedStatus Dao class for database operations
     */
    @Inject
    lateinit var uploadedStatusDao: UploadedStatusDao

    /**
     * View Model Factory.
     */
    @Inject
    lateinit var customSelectorViewModelFactory: CustomSelectorViewModelFactory

    /**
    * Coroutine Dispatchers and Scope.
    */
    private var defaultDispatcher : CoroutineDispatcher = Dispatchers.Default
    private var ioDispatcher : CoroutineDispatcher = Dispatchers.IO
    private val scope : CoroutineScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zoomable)
        ButterKnife.bind(this)

        prefs =  applicationContext.getSharedPreferences(
            ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY,
            MODE_PRIVATE
        )

        selectedImages = intent.getParcelableArrayListExtra(
            CustomSelectorConstants.TOTAL_SELECTED_IMAGES
        )
        position = intent.getIntExtra(CustomSelectorConstants.PRESENT_POSITION, 0)
        bucketId = intent.getLongExtra(CustomSelectorConstants.BUCKET_ID, 0L)
        viewModel = ViewModelProvider(this, customSelectorViewModelFactory).get(
            CustomSelectorViewModel::class.java
        )
        viewModel.fetchImages()
        viewModel.result.observe(this) {
            handleResult(it)
        }

        val origin = intent.getStringExtra(ZoomableActivityConstants.ORIGIN);

        /**
         * If origin is "null" it means that ZoomableActivity was created by the custom picker
         * (rather than by MediaDetailsFragment) so we need to show the first time popup in
         * full screen mode if needed.
         */
        if (origin == null) {
            if (prefs.getBoolean(CustomSelectorConstants.FULL_SCREEN_MODE_FIRST_LUNCH, true)) {
                // show welcome dialog on first launch
                showWelcomeDialog()
                prefs.edit().putBoolean(
                    CustomSelectorConstants.FULL_SCREEN_MODE_FIRST_LUNCH,
                    false
                ).apply()
            }
        }
        
        val backgroundColor = intent.getIntExtra(ZoomableActivityConstants.PHOTO_BACKGROUND_COLOR,
                MediaDetailFragment.DEFAULT_IMAGE_BACKGROUND_COLOR);

        if (backgroundColor != MediaDetailFragment.DEFAULT_IMAGE_BACKGROUND_COLOR) {
            photoBackgroundColor = backgroundColor
        }
    }

    /**
     * Show Full Screen Mode Welcome Dialog.
     */
    private fun showWelcomeDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.full_screen_mode_info_dialog)
        (dialog.findViewById(R.id.btn_ok) as Button).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Handle view model result.
     */
    private fun handleResult(result: Result) {
        if(result.status is CallbackStatus.SUCCESS){
            val images = result.images
            if(images.isNotEmpty()) {
                this@ZoomableActivity.images = ImageHelper.filterImages(images, bucketId)
                imageUri = if (this@ZoomableActivity.images.isNullOrEmpty()) {
                    intent.data as Uri
                } else {
                    this@ZoomableActivity.images!![position].uri
                }
                Timber.d("URL = $imageUri")
                init(imageUri)
                onSwipe()
            }
        }
        spinner?.let {
            it.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
        }
    }

    /**
     * Handle swap gestures. Ex. onSwipeLeft, onSwipeRight, onSwipeUp, onSwipeDown
     */
    private fun onSwipe() {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences(ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY, 0)
        val showAlreadyActionedImages =
            sharedPreferences.getBoolean(ImageHelper.SHOW_ALREADY_ACTIONED_IMAGES_PREFERENCE_KEY, true)

        if (!images.isNullOrEmpty()) {
            photo!!.setOnTouchListener(object : OnSwipeTouchListener(this) {
                // Swipe left to view next image in the folder. (if available)
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    onLeftSwiped(showAlreadyActionedImages)
                }

                // Swipe right to view previous image in the folder. (if available)
                override fun onSwipeRight() {
                    super.onSwipeRight()
                    onRightSwiped(showAlreadyActionedImages)
                }

                // Swipe up to select the picture (the equivalent of tapping it in non-fullscreen mode)
                // and show the next picture skipping pictures that have either already been uploaded or
                // marked as not for upload
                override fun onSwipeUp() {
                    super.onSwipeUp()
                    onUpSwiped()
                }

                // Swipe down to mark that picture as "Not for upload" (the equivalent of selecting it then
                // tapping "Mark as not for upload" in non-fullscreen mode), and show the next picture.
                override fun onSwipeDown() {
                    super.onSwipeDown()
                    onDownSwiped()
                }
            })
        }
    }

    /**
     * Handles down swipe action
     */
    private fun onDownSwiped() {
        if (photo?.zoomableController?.isIdentity == false)
            return

        scope.launch {
            val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                images!![position].uri,
                ioDispatcher,
                fileUtilsWrapper,
                contentResolver
            )
            var isUploaded = uploadedStatusDao.findByImageSHA1(imageSHA1, true)
            if (isUploaded > 0) {
                Toast.makeText(
                    this@ZoomableActivity,
                    getString(R.string.this_image_is_already_uploaded),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val imageModifiedSHA1 = CustomSelectorUtils.generateModifiedSHA1(
                    images!![position],
                    defaultDispatcher,
                    this@ZoomableActivity,
                    fileProcessor,
                    fileUtilsWrapper
                )
                isUploaded = uploadedStatusDao.findByModifiedImageSHA1(
                    imageModifiedSHA1,
                    true
                )
                if (isUploaded > 0) {
                    Toast.makeText(
                        this@ZoomableActivity,
                        getString(R.string.this_image_is_already_uploaded),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    insertInNotForUpload(images!![position])
                    Toast.makeText(
                        this@ZoomableActivity,
                        getString(R.string.image_marked_as_not_for_upload),
                        Toast.LENGTH_SHORT
                    ).show()
                    shouldRefresh = true
                    if (position < images!!.size - 1) {
                        position++
                        init(images!![position].uri)
                    } else {
                        Toast.makeText(
                            this@ZoomableActivity,
                            getString(R.string.no_more_images_found),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    /**
     * Handles up swipe action
     */
    private fun onUpSwiped() {
        if (photo?.zoomableController?.isIdentity == false)
            return

        scope.launch {
            val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                images!![position].uri,
                ioDispatcher,
                fileUtilsWrapper,
                contentResolver
            )
            var isNonActionable = notForUploadStatusDao.find(imageSHA1)
            if (isNonActionable > 0) {
                Toast.makeText(
                    this@ZoomableActivity,
                    getString(R.string.can_not_select_this_image_for_upload),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                isNonActionable =
                    uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                if (isNonActionable > 0) {
                    Toast.makeText(
                        this@ZoomableActivity,
                        getString(R.string.this_image_is_already_uploaded),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val imageModifiedSHA1 = CustomSelectorUtils.generateModifiedSHA1(
                        images!![position],
                        defaultDispatcher,
                        this@ZoomableActivity,
                        fileProcessor,
                        fileUtilsWrapper
                    )
                    isNonActionable = uploadedStatusDao.findByModifiedImageSHA1(
                        imageModifiedSHA1,
                        true
                    )
                    if (isNonActionable > 0) {
                        Toast.makeText(
                            this@ZoomableActivity,
                            getString(R.string.this_image_is_already_uploaded),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (!selectedImages!!.contains(images!![position])) {
                            selectedImages!!.add(images!![position])
                            Toast.makeText(
                                this@ZoomableActivity,
                                getString(R.string.image_selected),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        position = getNextActionableImage(position + 1)
                        init(images!![position].uri)
                    }
                }
            }
        }
    }

    /**
     * Handles right swipe action
     */
    private fun onRightSwiped(showAlreadyActionedImages: Boolean) {
        if (photo?.zoomableController?.isIdentity == false)
            return

        if (showAlreadyActionedImages) {
            if (position > 0) {
                position--
                init(images!![position].uri)
            } else {
                Toast.makeText(
                    this@ZoomableActivity,
                    getString(R.string.no_more_images_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            if (position > 0) {
                scope.launch {
                    position = getPreviousActionableImage(position - 1)
                    init(images!![position].uri)
                }
            } else {
                Toast.makeText(
                    this@ZoomableActivity,
                    getString(R.string.no_more_images_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Handles left swipe action
     */
    private fun onLeftSwiped(showAlreadyActionedImages: Boolean) {
        if (photo?.zoomableController?.isIdentity == false)
            return

        if (showAlreadyActionedImages) {
            if (position < images!!.size - 1) {
                position++
                init(images!![position].uri)
            } else {
                Toast.makeText(
                    this@ZoomableActivity,
                    getString(R.string.no_more_images_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            if (position < images!!.size - 1) {
                scope.launch {
                    position = getNextActionableImage(position + 1)
                    init(images!![position].uri)
                }
            } else {
                Toast.makeText(
                    this@ZoomableActivity,
                    getString(R.string.no_more_images_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Gets next actionable image.
     * Iterates from an index to the end of the folder and check whether the current image is
     * present in already uploaded table or in not for upload table,
     * and returns the first actionable image it can find.
     */
    private suspend fun getNextActionableImage(index: Int): Int {
        var nextPosition = position
        for(i in index until images!!.size){
            nextPosition = i
            val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                images!![i].uri,
                ioDispatcher,
                fileUtilsWrapper,
                contentResolver
            )
            var isNonActionable = notForUploadStatusDao.find(imageSHA1)
            if (isNonActionable <= 0) {
                isNonActionable = uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                if (isNonActionable <= 0) {
                    val imageModifiedSHA1 = CustomSelectorUtils.generateModifiedSHA1(
                        images!![i],
                        defaultDispatcher,
                        this@ZoomableActivity,
                        fileProcessor,
                        fileUtilsWrapper
                    )
                    isNonActionable = uploadedStatusDao.findByModifiedImageSHA1(
                        imageModifiedSHA1,
                        true
                    )
                    if (isNonActionable <= 0) {
                        return i
                    } else {
                        continue
                    }
                } else {
                    continue
                }
            } else {
                continue
            }
        }
        return nextPosition
    }

    /**
     * Gets previous actionable image.
     * Iterates from an index to the first image of the folder and check whether the current image
     * is present in already uploaded table or in not for upload table,
     * and returns the first actionable image it can find
     */
    private suspend fun getPreviousActionableImage(index: Int): Int {
        var previousPosition = position
        for(i in index downTo 0){
            previousPosition = i
            val imageSHA1 = CustomSelectorUtils.getImageSHA1(
                images!![i].uri,
                ioDispatcher,
                fileUtilsWrapper,
                contentResolver
            )
            var isNonActionable = notForUploadStatusDao.find(imageSHA1)
            if (isNonActionable <= 0) {
                isNonActionable = uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                if (isNonActionable <= 0) {
                    val imageModifiedSHA1 = CustomSelectorUtils.generateModifiedSHA1(
                        images!![i],
                        defaultDispatcher,
                        this@ZoomableActivity,
                        fileProcessor,
                        fileUtilsWrapper
                    )
                    isNonActionable = uploadedStatusDao.findByModifiedImageSHA1(
                        imageModifiedSHA1,
                        true
                    )
                    if (isNonActionable <= 0) {
                        return i
                    } else {
                        continue
                    }
                } else {
                    continue
                }
            } else {
                continue
            }
        }
        return previousPosition
    }

    /**
     * Unselect item UI
     */
    private fun itemUnselected() {
        selectedCount!!.visibility = View.INVISIBLE
    }

    /**
     * Select item UI
     */
    private fun itemSelected(i: Int) {
        selectedCount!!.visibility = View.VISIBLE
        selectedCount!!.text = i.toString()
    }

    /**
     * Get position of an image from list
     */
    private fun getImagePosition(list: ArrayList<Image>?, image: Image): Int {
        return list!!.indexOf(image)
    }

    /**
     * Two types of loading indicators have been added to the zoom activity:
     * 1.  An Indeterminate spinner for showing the time lapsed between dispatch of the image request
     * and starting to receiving the image.
     * 2.  ProgressBarDrawable that reflects how much image has been downloaded
     */
    private val loadingListener: ControllerListener<ImageInfo?> =
        object : BaseControllerListener<ImageInfo?>() {
            override fun onSubmit(id: String, callerContext: Any) {
                // Sometimes the spinner doesn't appear when rapidly switching between images, this fixes that
                spinner!!.visibility = View.VISIBLE
            }

            override fun onIntermediateImageSet(id: String, imageInfo: ImageInfo?) {
                spinner!!.visibility = View.GONE
            }

            override fun onFinalImageSet(
                id: String,
                imageInfo: ImageInfo?,
                animatable: Animatable?
            ) {
                spinner!!.visibility = View.GONE
            }
        }

    private fun init(imageUri: Uri?) {
        if (imageUri != null) {
            val hierarchy = GenericDraweeHierarchyBuilder.newInstance(resources)
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .setProgressBarImage(ProgressBarDrawable())
                .setProgressBarImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .build()
            photo!!.hierarchy = hierarchy
            photo!!.setAllowTouchInterceptionWhileZoomed(true)
            photo!!.setIsLongpressEnabled(false)
            photo!!.setTapListener(DoubleTapGestureListener(photo))
            val controller: DraweeController = Fresco.newDraweeControllerBuilder()
                .setUri(imageUri)
                .setControllerListener(loadingListener)
                .build()
            photo!!.controller = controller
            
            if (photoBackgroundColor != null) {
                photo!!.setBackgroundColor(photoBackgroundColor!!)
            }

            if (!images.isNullOrEmpty()) {
                val selectedIndex = getImagePosition(selectedImages, images!![position])
                val isSelected = selectedIndex != -1
                if (isSelected) {
                    itemSelected(selectedImages!!.size)
                } else {
                    itemUnselected()
                }
            }
        }
    }

    /**
     * Inserts an image in Not For Upload table
     */
    private suspend fun insertInNotForUpload(it: Image) {
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

    /**
     * Send selected images in fragment
     */
    override fun onBackPressed() {
        if (!images.isNullOrEmpty()) {
            val returnIntent = Intent()
            returnIntent.putParcelableArrayListExtra(
                CustomSelectorConstants.NEW_SELECTED_IMAGES,
                selectedImages
            )
            returnIntent.putExtra(SHOULD_REFRESH, shouldRefresh)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    object ZoomableActivityConstants  {
        /**
         * Key for Accessing Intent Data Named "Origin", The value indicates what fragment
         * ZoomableActivity was created by. It is null if ZoomableActivity was created by
         * the custom picker.
         */
        const val ORIGIN = "Origin";
        
        const val PHOTO_BACKGROUND_COLOR = "photo_background_color"
    }
}