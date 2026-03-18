package fr.free.nrw.commons.edit

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.rotationMatrix
import androidx.core.graphics.scaleMatrix
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModelProvider
import coil.load
import fr.free.nrw.commons.databinding.ActivityEditBinding
import fr.free.nrw.commons.utils.applyEdgeToEdgeBottomInsets
import fr.free.nrw.commons.utils.applyEdgeToEdgeTopPaddingInsets
import timber.log.Timber
import java.io.File
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * An activity class for editing and rotating images using LLJTran with EXIF attribute preservation.
 *
 * This activity allows loads an image, allows users to rotate it by 90-degree increments, and
 * save the edited image while preserving its EXIF attributes. The class includes methods
 * for initializing the UI, animating image rotations, copying EXIF data, and handling
 * the image-saving process.
 */
class EditActivity : AppCompatActivity() {
    private var imageUri = ""
    private lateinit var vm: EditViewModel
    private val sourceExifAttributeList = mutableListOf<Pair<String, String?>>()
    private lateinit var binding: ActivityEditBinding
    // variable to store the initial exif orientation
    private var startOrientation = 0

    private var isCropMode = false
    private var originalBitmapWidth = 0
    private var originalBitmapHeight = 0
    private var displayScale = 1f
    private var maxAvailableHeight = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = ""
        val intent = intent
        imageUri = intent.getStringExtra("image") ?: ""
        vm = ViewModelProvider(this)[EditViewModel::class.java]

        val sourceExif = try {
            ExifInterface(imageUri)
        } catch (e: Exception) {
            try {
                contentResolver.openInputStream(Uri.parse(imageUri))?.use {
                    ExifInterface(it)
                }
            } catch (e2: Exception) {
                Timber.e(e2, "Failed to read EXIF from URI")
                null
            }
        }

        // extracts the initial orientation
        val orientation = sourceExif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        startOrientation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        applyEdgeToEdgeBottomInsets(binding.root, false)
        binding.topBar.applyEdgeToEdgeTopPaddingInsets(WindowInsetsCompat.Type.statusBars())

        val exifTags =
            arrayOf(
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_IMAGE_LENGTH,
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.WHITE_BALANCE_AUTO,
                ExifInterface.WHITE_BALANCE_MANUAL,
            )
        for (tag in exifTags) {
            val attribute = sourceExif?.getAttribute(tag.toString())
            sourceExifAttributeList.add(Pair(tag.toString(), attribute))
        }

        if (imageUri.substringBefore("?").endsWith(".svg", ignoreCase = true)) {
            Toast.makeText(this, "SVG files cannot be edited", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        init()
    }

    /**
     * Initializes the ImageView and associated UI elements.
     *
     * This function sets up the ImageView for displaying an image, adjusts its view bounds,
     * and scales the initial image to fit within the ImageView. It also sets click listeners
     * for the "Rotate" and "Save" buttons.
     */
    private fun init() {
        binding.iv.adjustViewBounds = true
        binding.iv.scaleType = ImageView.ScaleType.MATRIX
        if (imageUri.substringBefore("?").endsWith(".svg", ignoreCase = true)) {
            binding.rotateBtn.isEnabled = false
            binding.iv.load(imageUri) {
                listener(
                    onSuccess = { _, result ->
                        val drawable = result.drawable
                        val scale = binding.iv.measuredWidth.toFloat() / drawable.intrinsicWidth.toFloat()
                        binding.iv.layoutParams.height = (scale * drawable.intrinsicHeight).toInt()
                        binding.iv.imageMatrix = scaleMatrix(scale, scale)
                    }
                )
            }
        } else {
            binding.iv.post {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(imageUri, options)
            val bitmapWidth = options.outWidth
            val bitmapHeight = options.outHeight

            // Store original dimensions for crop calculation
            originalBitmapWidth = bitmapWidth
            originalBitmapHeight = bitmapHeight

            maxAvailableHeight = binding.iv.measuredHeight.toFloat()

            // Check if the bitmap dimensions exceed a certain threshold
            val maxBitmapSize = 2000
            var finalBitmap: Bitmap?
            if (bitmapWidth > maxBitmapSize || bitmapHeight > maxBitmapSize) {
                val scaleFactor = calculateScaleFactor(bitmapWidth, bitmapHeight, maxBitmapSize)
                options.inSampleSize = scaleFactor
                options.inJustDecodeBounds = false
                finalBitmap = BitmapFactory.decodeFile(imageUri, options)
            } else {
                options.inJustDecodeBounds = false
                finalBitmap = BitmapFactory.decodeFile(imageUri, options)
            }

            if (finalBitmap != null) {
                binding.iv.setImageBitmap(finalBitmap)
                binding.iv.rotation = 0f
                imageRotation = startOrientation
                val viewWidth = binding.iv.measuredWidth.toFloat()
                val bmpWidth = finalBitmap.width.toFloat()
                val bmpHeight = finalBitmap.height.toFloat()

                val matrix = Matrix()
                val isRotated90 = (startOrientation == 90 || startOrientation == 270)

                val scale = if (isRotated90) {
                    min(viewWidth / bmpHeight, maxAvailableHeight / bmpWidth)
                } else {
                    min(viewWidth / bmpWidth, maxAvailableHeight / bmpHeight)
                }

                val viewHeight = if (isRotated90) {
                    min((scale * bmpWidth).toInt(), maxAvailableHeight.toInt())
                } else {
                    min((scale * bmpHeight).toInt(), maxAvailableHeight.toInt())
                }

                binding.iv.layoutParams.height = viewHeight
                // rotate around center of the bitmap
                matrix.postRotate(startOrientation.toFloat(), bmpWidth / 2, bmpHeight / 2)
                matrix.postScale(scale, scale, bmpWidth / 2, bmpHeight / 2)
                val bmpCenterX = bmpWidth / 2
                val bmpCenterY = bmpHeight / 2
                val viewCenterX = viewWidth / 2
                val viewCenterY = viewHeight.toFloat() / 2

                matrix.postTranslate(viewCenterX - bmpCenterX, viewCenterY - bmpCenterY)

                binding.iv.imageMatrix = matrix
            }

        }}
        binding.rotateBtn.setOnClickListener {
            // Allow rotation while in crop mode - overlay will update after animation
            animateImageHeight()
        }
        binding.cropBtn.setOnClickListener {
            toggleCropMode()
        }
        binding.btnSave.setOnClickListener {
            saveEditedImage()
        }
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    /**
     * Toggles crop mode on and off.
     */
    private fun toggleCropMode() {
        if (isCropMode) {
            exitCropMode()
        } else {
            enterCropMode()
        }
    }

    /**
     * Enters crop mode, showing the crop overlay.
     */
    private fun enterCropMode() {
        isCropMode = true
        binding.cropOverlay.visibility = View.VISIBLE
        // Allow rotation while in crop mode for flexibility

        binding.iv.post {
            updateCropOverlayBounds()
        }
    }

    /**
     * Updates crop overlay bounds based on current image display (works with any rotation).
     */
    private fun updateCropOverlayBounds() {
        val drawable = binding.iv.drawable ?: return
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val matrix = binding.iv.imageMatrix

        val drawableRect = RectF(0f, 0f, drawableWidth, drawableHeight)
        matrix.mapRect(drawableRect)

        // Use getLocationInWindow to correctly compute offset between views
        val ivLoc = IntArray(2)
        val overlayLoc = IntArray(2)
        binding.iv.getLocationInWindow(ivLoc)
        binding.cropOverlay.getLocationInWindow(overlayLoc)

        val offsetX = (ivLoc[0] - overlayLoc[0]).toFloat()
        val offsetY = (ivLoc[1] - overlayLoc[1]).toFloat()

        binding.cropOverlay.setImageBounds(
            offsetX + drawableRect.left,
            offsetY + drawableRect.top,
            offsetX + drawableRect.right,
            offsetY + drawableRect.bottom
        )
    }

    /**
     * Exits crop mode, hiding the crop overlay.
     */
    private fun exitCropMode() {
        isCropMode = false
        binding.cropOverlay.visibility = View.GONE
    }

    /**
     * Saves the edited image (with rotation and/or crop applied).
     */
    private fun saveEditedImage() {
        val filePath = imageUri.toUri().path
        var file = filePath?.let { File(it) }

        // Apply rotation first if needed
        if (imageRotation != 0 && file != null) {
            val rotatedImage = vm.rotateImage(imageRotation, file, applicationContext.cacheDir)
            if (rotatedImage == null) {
                Toast.makeText(this, "Failed to rotate image", Toast.LENGTH_LONG).show()
                return
            }
            file = rotatedImage
        }

        // Apply crop if in crop mode
        if (isCropMode && file != null) {
            // Read ACTUAL dimensions of the (possibly rotated) file
            val fileDimensions = getImageFileDimensions(file)
            if (fileDimensions == null) {
                Toast.makeText(this, "Failed to read image dimensions", Toast.LENGTH_LONG).show()
                return
            }

            val (actualWidth, actualHeight) = fileDimensions
            Timber.d("Actual file dimensions after rotation: ${actualWidth}x${actualHeight}")

            val cropRect = binding.cropOverlay.getCropRect()
            val cropCoords = convertViewCropToImageCrop(cropRect, actualWidth, actualHeight)

            if (cropCoords != null) {
                val croppedImage = vm.cropImage(
                    file,
                    cropCoords.left,
                    cropCoords.top,
                    cropCoords.width,
                    cropCoords.height,
                    applicationContext.cacheDir
                )
                if (croppedImage == null) {
                    Toast.makeText(this, "Failed to crop image", Toast.LENGTH_LONG).show()
                    return
                }
                file = croppedImage
            }
        }

        // Copy EXIF data
        if (file?.path != null) {
            val editedImageExif = ExifInterface(file.path)
            copyExifData(editedImageExif)
        }

        val resultIntent = Intent()
        resultIntent.putExtra("editedImageFilePath", file?.toUri()?.path ?: "Error")
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Reads the actual dimensions of an image file.
     * Returns Pair(width, height) or null if unable to read.
     */
    private fun getImageFileDimensions(file: File): Pair<Int, Int>? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.path, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                Pair(options.outWidth, options.outHeight)
            } else null
        } catch (e: Exception) {
            Timber.e(e, "Failed to read image dimensions")
            null
        }
    }

    /**
     * Converts crop rectangle from view coordinates to rotated image coordinates.
     * Uses percentage-based mapping with actual file dimensions.
     *
     * @param viewCropRect The crop rectangle in view coordinates
     * @param actualFileWidth The actual width of the (possibly rotated) file
     * @param actualFileHeight The actual height of the (possibly rotated) file
     */
    private fun convertViewCropToImageCrop(
        viewCropRect: RectF,
        actualFileWidth: Int,
        actualFileHeight: Int
    ): CropCoordinates? {
        val drawable = binding.iv.drawable ?: return null
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val matrix = binding.iv.imageMatrix

        // Get the displayed bounds (after matrix transformation)
        val displayedRect = RectF(0f, 0f, drawableWidth, drawableHeight)
        matrix.mapRect(displayedRect)

        // Use getLocationInWindow to correctly compute offset between views
        val ivLoc = IntArray(2)
        val overlayLoc = IntArray(2)
        binding.iv.getLocationInWindow(ivLoc)
        binding.cropOverlay.getLocationInWindow(overlayLoc)

        val offsetX = (ivLoc[0] - overlayLoc[0]).toFloat()
        val offsetY = (ivLoc[1] - overlayLoc[1]).toFloat()
        displayedRect.offset(offsetX, offsetY)

        // Guard against zero-size displayed rect
        if (displayedRect.width() <= 0 || displayedRect.height() <= 0) return null

        // Calculate crop as percentage of displayed bounds
        val leftPercent = ((viewCropRect.left - displayedRect.left) / displayedRect.width())
            .coerceIn(0f, 1f)
        val topPercent = ((viewCropRect.top - displayedRect.top) / displayedRect.height())
            .coerceIn(0f, 1f)
        val rightPercent = ((viewCropRect.right - displayedRect.left) / displayedRect.width())
            .coerceIn(0f, 1f)
        val bottomPercent = ((viewCropRect.bottom - displayedRect.top) / displayedRect.height())
            .coerceIn(0f, 1f)

        // Apply percentages to ACTUAL file dimensions
        val cropLeft = (leftPercent * actualFileWidth).roundToInt().coerceIn(0, actualFileWidth - 1)
        val cropTop = (topPercent * actualFileHeight).roundToInt().coerceIn(0, actualFileHeight - 1)
        val cropRight = (rightPercent * actualFileWidth).roundToInt().coerceIn(1, actualFileWidth)
        val cropBottom = (bottomPercent * actualFileHeight).roundToInt().coerceIn(1, actualFileHeight)

        val width = (cropRight - cropLeft).coerceAtLeast(1)
        val height = (cropBottom - cropTop).coerceAtLeast(1)

        // Final validation: ensure crop doesn't exceed file bounds
        val validLeft = cropLeft.coerceIn(0, actualFileWidth - width)
        val validTop = cropTop.coerceIn(0, actualFileHeight - height)

        Timber.d("Crop conversion: file=${actualFileWidth}x${actualFileHeight}, " +
                 "crop=($validLeft, $validTop, $width, $height)")

        return CropCoordinates(validLeft, validTop, width, height)
    }

    var imageRotation = 0

    /**
     * Data class representing crop coordinates.
     */
    private data class CropCoordinates(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    )

    /**
     * Animates the height, rotation, and scale of an ImageView to provide a smooth
     * transition effect when rotating an image by 90 degrees.
     *
     * This function calculates the new height, rotation, and scale for the ImageView
     * based on the current image rotation angle and animates the changes using a
     * ValueAnimator. It also disables a rotate button during the animation to prevent
     * further rotation actions.
     */
    private fun animateImageHeight() {
        val drawableWidth: Float =
            binding.iv
                .getDrawable()
                .intrinsicWidth
                .toFloat()
        val drawableHeight: Float =
            binding.iv
                .getDrawable()
                .intrinsicHeight
                .toFloat()
        val viewWidth: Float = binding.iv.measuredWidth.toFloat()
        val viewHeight: Float = binding.iv.measuredHeight.toFloat()
        val rotation = imageRotation % 360
        val newRotation = rotation + 90
        // if the the user taps "save" before the animation finishes, it will save the new rotation.
        imageRotation = newRotation % 360

        val newViewHeight: Int
        val imageScale: Float
        val newImageScale: Float

        Timber.d("Rotation $rotation")
        Timber.d("new Rotation $newRotation")

        when (rotation) {
            0, 180 -> {
                imageScale = viewWidth / drawableWidth
                newImageScale = viewWidth / drawableHeight
                newViewHeight = (drawableWidth * newImageScale).toInt()
            }
            90, 270 -> {
                imageScale = viewWidth / drawableHeight
                newImageScale = viewWidth / drawableWidth
                newViewHeight = (drawableHeight * newImageScale).toInt()
            }
            else -> {
                throw
                UnsupportedOperationException(
                    "rotation can 0, 90, 180 or 270. $rotation is unsupported"
                )
            }
        }

        val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(1000L)

        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addListener(
            object : AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    binding.rotateBtn.setEnabled(false)
                }

                override fun onAnimationEnd(animation: Animator) {
                    imageRotation = newRotation % 360
                    binding.rotateBtn.setEnabled(true)
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            },
        )

        animator.addUpdateListener { animation ->
            val animVal = animation.animatedValue as Float
            val complementaryAnimVal = 1 - animVal
            val animatedHeight =
                (complementaryAnimVal * viewHeight + animVal * newViewHeight).toInt()
            val animatedScale = complementaryAnimVal * imageScale + animVal * newImageScale
            val animatedRotation = complementaryAnimVal * rotation + animVal * newRotation
            binding.iv.layoutParams.height = animatedHeight
            val matrix: Matrix =
                rotationMatrix(
                    animatedRotation,
                    drawableWidth / 2,
                    drawableHeight / 2,
                )
            matrix.postScale(
                animatedScale,
                animatedScale,
                drawableWidth / 2,
                drawableHeight / 2,
            )
            matrix.postTranslate(
                -(drawableWidth - binding.iv.measuredWidth) / 2,
                -(drawableHeight - binding.iv.measuredHeight) / 2,
            )
            binding.iv.setImageMatrix(matrix)
            binding.iv.requestLayout()
        }

        animator.start()
    }

    /**
     * Rotates and edits the current image, copies EXIF data, and returns the edited image path.
     *
     * This function retrieves the path of the current image specified by `imageUri`,
     * rotates it based on the `imageRotation` angle using the `rotateImage` method
     * from the `vm`, and updates the EXIF attributes of the
     * rotated image based on the `sourceExifAttributeList`. It then copies the EXIF data
     * using the `copyExifData` method, creates an Intent to return the edited image's file path
     * as a result, and finishes the current activity.
     */
    fun getRotatedImage() {
        val filePath = imageUri.toUri().path
        val file = filePath?.let { File(it) }

        val rotatedImage = file?.let { vm.rotateImage(imageRotation, it, applicationContext.cacheDir) }
        if (rotatedImage == null) {
            Toast.makeText(this, "Failed to rotate to image", Toast.LENGTH_LONG).show()
        }
        val editedImageExif: ExifInterface?
        if (rotatedImage?.path != null) {
            editedImageExif = ExifInterface(rotatedImage.path)
            copyExifData(editedImageExif)
        }
        val resultIntent = Intent()
        resultIntent.putExtra("editedImageFilePath", rotatedImage?.toUri()?.path ?: "Error")
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Copies EXIF data from sourceExifAttributeList to the provided ExifInterface object.
     *
     * This function iterates over the `sourceExifAttributeList` and sets the EXIF attributes
     * on the provided `editedImageExif` object.
     *
     * @param editedImageExif The ExifInterface object for the edited image.
     */
    private fun copyExifData(editedImageExif: ExifInterface?) {
        for (attr in sourceExifAttributeList) {
            Timber.d("Value is ${attr.second}")
            editedImageExif!!.setAttribute(attr.first, attr.second)
            Timber.d("Value is ${attr.second}")
        }

        editedImageExif?.saveAttributes()
    }

    /**
     * Calculates the scale factor to be used for scaling down a bitmap based on its original
     *  dimensions and the maximum allowed size.
     * @param originalWidth  The original width of the bitmap.
     * @param originalHeight The original height of the bitmap.
     * @param maxSize        The maximum allowed size for either width or height.
     * @return The scale factor to be used for scaling down the bitmap.
     *         If the bitmap is smaller than or equal to the maximum size in both dimensions,
     *         the scale factor is 1.
     *         If the bitmap is larger than the maximum size in either dimension,
     *         the scale factor is calculated as the largest power of 2 that is less than or equal
     *         to the ratio of the original dimension to the maximum size.
     *         The scale factor ensures that the scaled bitmap will fit within the maximum size
     *         while maintaining aspect ratio.
     */
    private fun calculateScaleFactor(
        originalWidth: Int,
        originalHeight: Int,
        maxSize: Int,
    ): Int {
        var scaleFactor = 1

        if (originalWidth > maxSize || originalHeight > maxSize) {
            // Calculate the largest power of 2 that is less than or equal to the desired
            // width and height
            val widthRatio = ceil((originalWidth.toDouble() / maxSize.toDouble())).toInt()
            val heightRatio = ceil((originalHeight.toDouble() / maxSize.toDouble())).toInt()

            scaleFactor = if (widthRatio > heightRatio) widthRatio else heightRatio
        }

        return scaleFactor
    }
}
