package fr.free.nrw.commons.edit

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.rotationMatrix
import androidx.core.graphics.scaleMatrix
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.databinding.ActivityEditBinding
import timber.log.Timber
import java.io.File

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = ""
        val intent = intent
        imageUri = intent.getStringExtra("image") ?: ""
        vm = ViewModelProvider(this).get(EditViewModel::class.java)
        val sourceExif = imageUri.toUri().path?.let { ExifInterface(it) }
        val exifTags = arrayOf(
            ExifInterface.TAG_APERTURE,
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
            ExifInterface.TAG_ISO,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.WHITEBALANCE_AUTO,
            ExifInterface.WHITEBALANCE_MANUAL
        )
        for (tag in exifTags) {
            val attribute = sourceExif?.getAttribute(tag.toString())
            sourceExifAttributeList.add(Pair(tag.toString(), attribute))
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
        binding.iv.post(Runnable {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imageUri, options)

            val bitmapWidth = options.outWidth
            val bitmapHeight = options.outHeight

            // Check if the bitmap dimensions exceed a certain threshold
            val maxBitmapSize = 2000 // Set your maximum size here
            if (bitmapWidth > maxBitmapSize || bitmapHeight > maxBitmapSize) {
                val scaleFactor = calculateScaleFactor(bitmapWidth, bitmapHeight, maxBitmapSize)
                options.inSampleSize = scaleFactor
                options.inJustDecodeBounds = false
                val scaledBitmap = BitmapFactory.decodeFile(imageUri, options)
                binding.iv.setImageBitmap(scaledBitmap)
                // Update the ImageView with the scaled bitmap
                val scale = binding.iv.measuredWidth.toFloat() / scaledBitmap.width.toFloat()
                binding.iv.layoutParams.height = (scale * scaledBitmap.height).toInt()
                binding.iv.imageMatrix = scaleMatrix(scale, scale)
            } else {

                options.inJustDecodeBounds = false
                val bitmap = BitmapFactory.decodeFile(imageUri, options)
                binding.iv.setImageBitmap(bitmap)

                val scale = binding.iv.measuredWidth.toFloat() / bitmapWidth.toFloat()
                binding.iv.layoutParams.height = (scale * bitmapHeight).toInt()
                binding.iv.imageMatrix = scaleMatrix(scale, scale)
            }
        })
        binding.rotateBtn.setOnClickListener {
            animateImageHeight()
        }
        binding.btnSave.setOnClickListener {
            getRotatedImage()
        }
    }

    var imageRotation = 0

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
        val drawableWidth: Float = binding.iv.getDrawable().getIntrinsicWidth().toFloat()
        val drawableHeight: Float = binding.iv.getDrawable().getIntrinsicHeight().toFloat()
        val viewWidth: Float = binding.iv.getMeasuredWidth().toFloat()
        val viewHeight: Float = binding.iv.getMeasuredHeight().toFloat()
        val rotation = imageRotation % 360
        val newRotation = rotation + 90

        val newViewHeight: Int
        val imageScale: Float
        val newImageScale: Float

        Timber.d("Rotation $rotation")
        Timber.d("new Rotation $newRotation")


        if (rotation == 0 || rotation == 180) {
            imageScale = viewWidth / drawableWidth
            newImageScale = viewWidth / drawableHeight
            newViewHeight = (drawableWidth * newImageScale).toInt()
        } else if (rotation == 90 || rotation == 270) {
            imageScale = viewWidth / drawableHeight
            newImageScale = viewWidth / drawableWidth
            newViewHeight = (drawableHeight * newImageScale).toInt()
        } else {
            throw UnsupportedOperationException("rotation can 0, 90, 180 or 270. \${rotation} is unsupported")
        }

        val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(1000L)

        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addListener(object : AnimatorListener {
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

        })

        animator.addUpdateListener { animation ->
            val animVal = animation.animatedValue as Float
            val complementaryAnimVal = 1 - animVal
            val animatedHeight =
                (complementaryAnimVal * viewHeight + animVal * newViewHeight).toInt()
            val animatedScale = complementaryAnimVal * imageScale + animVal * newImageScale
            val animatedRotation = complementaryAnimVal * rotation + animVal * newRotation
            binding.iv.getLayoutParams().height = animatedHeight
            val matrix: Matrix = rotationMatrix(
                animatedRotation,
                drawableWidth / 2,
                drawableHeight / 2
            )
            matrix.postScale(
                animatedScale,
                animatedScale,
                drawableWidth / 2,
                drawableHeight / 2
            )
            matrix.postTranslate(
                -(drawableWidth - binding.iv.getMeasuredWidth()) / 2,
                -(drawableHeight - binding.iv.getMeasuredHeight()) / 2
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


        val rotatedImage = file?.let { vm.rotateImage(imageRotation, it) }
        if (rotatedImage == null) {
            Toast.makeText(this, "Failed to rotate to image", Toast.LENGTH_LONG).show()
        }
        val editedImageExif = rotatedImage?.path?.let { ExifInterface(it) }
        copyExifData(editedImageExif)
        val resultIntent = Intent()
        resultIntent.putExtra("editedImageFilePath", rotatedImage?.toUri()?.path ?: "Error");
        setResult(RESULT_OK, resultIntent);
        finish();
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
            Log.d("Tag is  ${attr.first}", "Value is ${attr.second}")
            editedImageExif!!.setAttribute(attr.first, attr.second)
            Log.d("Tag is ${attr.first}", "Value is ${attr.second}")
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
    private fun calculateScaleFactor(originalWidth: Int, originalHeight: Int, maxSize: Int): Int {
        var scaleFactor = 1

        if (originalWidth > maxSize || originalHeight > maxSize) {
            // Calculate the largest power of 2 that is less than or equal to the desired width and height
            val widthRatio = Math.ceil((originalWidth.toDouble() / maxSize.toDouble())).toInt()
            val heightRatio = Math.ceil((originalHeight.toDouble() / maxSize.toDouble())).toInt()

            scaleFactor = if (widthRatio > heightRatio) widthRatio else heightRatio
        }

        return scaleFactor
    }

    fun getRotatedImage(view: View) {}


}