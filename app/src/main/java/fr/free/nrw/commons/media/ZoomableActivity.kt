package fr.free.nrw.commons.media

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import fr.free.nrw.commons.customselector.helper.OnSwipeTouchListener
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.media.zoomControllers.zoomable.DoubleTapGestureListener
import fr.free.nrw.commons.media.zoomControllers.zoomable.ZoomableDraweeView
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.upload.FileProcessor
import fr.free.nrw.commons.upload.FileUtilsWrapper
import fr.free.nrw.commons.utils.CustomSelectorUtils
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject

class ZoomableActivity : BaseActivity() {

    private lateinit var imageUri: Uri

    @JvmField
    @BindView(R.id.zoomable)
    var photo: ZoomableDraweeView? = null

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
    * Coroutine Dispatchers and Scope.
    */
    private var defaultDispatcher : CoroutineDispatcher = Dispatchers.Default
    private var ioDispatcher : CoroutineDispatcher = Dispatchers.IO
    private val scope : CoroutineScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        images = intent.getParcelableArrayListExtra(
            CustomSelectorConstants.TOTAL_IMAGES
        )
        selectedImages = intent.getParcelableArrayListExtra(
            CustomSelectorConstants.TOTAL_SELECTED_IMAGES
        )
        position = intent.getIntExtra(CustomSelectorConstants.PRESENT_POSITION, 0)
        imageUri = if (images.isNullOrEmpty()) {
            intent.data as Uri
        } else {
            images!![position].uri
        }
        Timber.d("URl = $imageUri")
        setContentView(R.layout.activity_zoomable)
        ButterKnife.bind(this)
        init(imageUri)
        onSwap()
    }

    /**
     * Handle swap gestures. Ex. onSwipeLeft, onSwipeRight, onSwipeUp, onSwipeDown
     */
    private fun onSwap() {
        if (!images.isNullOrEmpty()) {
            photo!!.setOnTouchListener(object : OnSwipeTouchListener(this) {
                override fun onSwipeLeft() {
                    super.onSwipeLeft()
                    if (position < images!!.size - 1) {
                        position++
                        init(images!![position].uri)
                    } else {
                        Toast.makeText(
                            this@ZoomableActivity,
                            "No more images found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onSwipeRight() {
                    super.onSwipeRight()
                    if (position > 0) {
                        position--
                        init(images!![position].uri)
                    } else {
                        Toast.makeText(
                            this@ZoomableActivity,
                            "No more images found",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onSwipeUp() {
                    super.onSwipeUp()
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
                                "Can't select this image for upload", Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            isNonActionable =
                                uploadedStatusDao.findByImageSHA1(imageSHA1, true)
                            if (isNonActionable > 0) {
                                Toast.makeText(
                                    this@ZoomableActivity,
                                    "Can't select this image for upload", Toast.LENGTH_SHORT
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
                                        "Can't select this image for upload",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    if (!selectedImages!!.contains(images!![position])) {
                                        selectedImages!!.add(images!![position])
                                    }
                                    position = getNextActionableImage(position + 1)
                                    init(images!![position].uri)
                                }
                            }
                        }
                    }
                }

                override fun onSwipeDown() {
                    super.onSwipeDown()
                    scope.launch {
                        insertInNotForUpload(images!![position])
                        if (position < images!!.size - 1) {
                            position++
                            init(images!![position].uri)
                        } else {
                            Toast.makeText(
                                this@ZoomableActivity,
                                "No more images found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        }
    }

    /**
     * Gets next actionable image
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
     * Get index from list
     */
    private fun getIndex(list: ArrayList<Image>?, image: Image): Int {
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

            if (!images.isNullOrEmpty()) {
                val selectedIndex = getIndex(selectedImages, images!![position])
                val isSelected = selectedIndex != -1
                if (isSelected) {
                    itemSelected(selectedIndex + 1)
                } else {
                    itemUnselected()
                }
            }
        }
    }

    /**
     * Inserts an image in Not For Upload Database
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
                imageSHA1,
                true
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
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
        super.onBackPressed()
    }
}