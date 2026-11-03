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
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.controller.ControllerListener
import com.facebook.drawee.drawable.ProgressBarDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.imagepipeline.image.ImageInfo
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants
import fr.free.nrw.commons.customselector.helper.CustomSelectorConstants.SHOULD_REFRESH
import fr.free.nrw.commons.customselector.helper.ImageHelper
import fr.free.nrw.commons.customselector.model.CallbackStatus
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.model.Result
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorViewModel
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorViewModelFactory
import fr.free.nrw.commons.databinding.ActivityZoomableBinding
import fr.free.nrw.commons.media.zoomControllers.zoomable.DoubleTapGestureListener
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.applyEdgeToEdgeTopPaddingInsets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
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

    internal lateinit var binding: ActivityZoomableBinding

    /**
     * Swipe handler.
     */
    private lateinit var swipeHandler: ZoomableActivitySwipeHandler

    var photoBackgroundColor: Int? = null

    /**
     * Total images present in folder
     */
    internal var images: ArrayList<Image>? = null

    /**
     * Total selected images present in folder
     */
    internal var selectedImages: ArrayList<Image>? = null

    /**
     * Present position of the image
     */
    internal var position = 0

    /**
     * Present bucket ID
     */
    private var bucketId: Long = 0L

    /**
     * Determines whether the adapter should refresh
     */
    internal var shouldRefresh = false

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
    internal var defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
    internal var ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    internal val scope: CoroutineScope = MainScope()
    private var isSystemBarsVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityZoomableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.applyEdgeToEdgeTopPaddingInsets(WindowInsetsCompat.Type.statusBars())
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!images.isNullOrEmpty()) {
                        val returnIntent = Intent()
                        returnIntent.putParcelableArrayListExtra(
                            CustomSelectorConstants.NEW_SELECTED_IMAGES,
                            selectedImages,
                        )
                        returnIntent.putExtra(SHOULD_REFRESH, shouldRefresh)
                        setResult(Activity.RESULT_OK, returnIntent)
                        finish()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            },
        )
        swipeHandler = ZoomableActivitySwipeHandler(this)
        prefs =
            applicationContext.getSharedPreferences(
                ImageHelper.CUSTOM_SELECTOR_PREFERENCE_KEY,
                MODE_PRIVATE,
            )

        selectedImages =
            intent.getParcelableArrayListExtra(
                CustomSelectorConstants.TOTAL_SELECTED_IMAGES,
            )
        position = intent.getIntExtra(CustomSelectorConstants.PRESENT_POSITION, 0)
        bucketId = intent.getLongExtra(CustomSelectorConstants.BUCKET_ID, 0L)
        viewModel =
            ViewModelProvider(this, customSelectorViewModelFactory).get(
                CustomSelectorViewModel::class.java,
            )
        viewModel.fetchImages()
        viewModel.result.observe(this) {
            handleResult(it)
        }

        val origin = intent.getStringExtra(ZoomableActivityConstants.ORIGIN)

        /**
         * If origin is "null" it means that ZoomableActivity was created by the custom picker
         * (rather than by MediaDetailsFragment) so we need to show the first time popup in
         * full screen mode if needed.
         */
        if (origin == null) {
            if (prefs.getBoolean(CustomSelectorConstants.FULL_SCREEN_MODE_FIRST_LUNCH, true)) {
                // show welcome dialog on first launch
                showWelcomeDialog()
                prefs
                    .edit()
                    .putBoolean(
                        CustomSelectorConstants.FULL_SCREEN_MODE_FIRST_LUNCH,
                        false,
                    ).apply()
            }
        }

        val backgroundColor =
            intent.getIntExtra(
                ZoomableActivityConstants.PHOTO_BACKGROUND_COLOR,
                MediaDetailFragment.DEFAULT_IMAGE_BACKGROUND_COLOR,
            )

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
        dialog.setCancelable(false)
        (dialog.findViewById(R.id.btn_ok) as Button).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    /**
     * Handle view model result.
     */
    private fun handleResult(result: Result) {
        if (result.status is CallbackStatus.SUCCESS) {
            val images = result.images
            if (images.isNotEmpty()) {
                this@ZoomableActivity.images = ImageHelper.filterImages(images, bucketId)
                imageUri =
                    if (this@ZoomableActivity.images.isNullOrEmpty()) {
                        intent.data as Uri
                    } else {
                        this@ZoomableActivity.images!![position].uri
                    }
                Timber.d("URL = $imageUri")
                init(imageUri)
                swipeHandler.onSwipe()
            }
        }
        binding.zoomProgressBar.let {
            it.visibility = if (result.status is CallbackStatus.FETCHING) View.VISIBLE else View.GONE
        }
    }

    /**
     * Unselect item UI
     */
    private fun itemUnselected() {
        binding.selectionCount.visibility = View.INVISIBLE
    }

    /**
     * Select item UI
     */
    private fun itemSelected(i: Int) {
        binding.selectionCount.visibility = View.VISIBLE
        binding.selectionCount.text = i.toString()
    }

    /**
     * Get position of an image from list
     */
    private fun getImagePosition(
        list: ArrayList<Image>?,
        image: Image,
    ): Int = list!!.indexOf(image)

    /**
     * Two types of loading indicators have been added to the zoom activity:
     * 1.  An Indeterminate spinner for showing the time lapsed between dispatch of the image request
     * and starting to receiving the image.
     * 2.  ProgressBarDrawable that reflects how much image has been downloaded
     */
    private val loadingListener: ControllerListener<ImageInfo?> =
        object : BaseControllerListener<ImageInfo?>() {
            override fun onSubmit(
                id: String,
                callerContext: Any,
            ) {
                // Sometimes the spinner doesn't appear when rapidly switching between images, this fixes that
                binding.zoomProgressBar.visibility = View.VISIBLE
            }

            override fun onIntermediateImageSet(
                id: String,
                imageInfo: ImageInfo?,
            ) {
                binding.zoomProgressBar.visibility = View.GONE
            }

            override fun onFinalImageSet(
                id: String,
                imageInfo: ImageInfo?,
                animatable: Animatable?,
            ) {
                binding.zoomProgressBar.visibility = View.GONE
            }
        }

    internal fun init(imageUri: Uri?) {
        if (imageUri != null) {
            val hierarchy =
                GenericDraweeHierarchyBuilder
                    .newInstance(resources)
                    .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                    .setProgressBarImage(ProgressBarDrawable())
                    .setProgressBarImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                    .build()
            with(binding.zoomable) {
                setHierarchy(hierarchy)
                setAllowTouchInterceptionWhileZoomed(true)
                setIsLongpressEnabled(false)
                setTapListener(
                    DoubleTapGestureListener(this) {
                        toggleSystemBars()
                    },
                )
            }
            val controller: DraweeController =
                Fresco
                    .newDraweeControllerBuilder()
                    .setUri(imageUri)
                    .setControllerListener(loadingListener)
                    .build()
            binding.zoomable.controller = controller

            if (photoBackgroundColor != null) {
                binding.zoomable.setBackgroundColor(photoBackgroundColor!!)
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

    private fun toggleSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isSystemBarsVisible) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            binding.topBar.visibility = View.GONE
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            binding.topBar.visibility = View.VISIBLE
        }
        isSystemBarsVisible = !isSystemBarsVisible
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    object ZoomableActivityConstants {
        /**
         * Key for Accessing Intent Data Named "Origin", The value indicates what fragment
         * ZoomableActivity was created by. It is null if ZoomableActivity was created by
         * the custom picker.
         */
        const val ORIGIN = "Origin"

        const val PHOTO_BACKGROUND_COLOR = "photo_background_color"
    }
}
