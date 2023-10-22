package fr.free.nrw.commons.edit

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.os.Bundle
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.rotationMatrix
import androidx.core.graphics.scaleMatrix
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import kotlinx.android.synthetic.main.activity_edit.btn_save
import kotlinx.android.synthetic.main.activity_edit.iv
import kotlinx.android.synthetic.main.activity_edit.rotate_btn
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
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
        iv.adjustViewBounds = true
        iv.scaleType = ImageView.ScaleType.MATRIX
        iv.post(Runnable {
            val bitmap = BitmapFactory.decodeFile(imageUri)
            iv.setImageBitmap(bitmap)
            if (bitmap.width > 0) {
                val scale =
                    iv.measuredWidth.toFloat() / (iv.drawable as BitmapDrawable).bitmap.width.toFloat()
                iv.layoutParams.height =
                    (scale * (iv.drawable as BitmapDrawable).bitmap.height).toInt()
                iv.imageMatrix = scaleMatrix(scale, scale)
            }
        })
        rotate_btn.setOnClickListener {
            animateImageHeight()
        }
        btn_save.setOnClickListener {
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
        val drawableWidth: Float = iv.getDrawable().getIntrinsicWidth().toFloat()
        val drawableHeight: Float = iv.getDrawable().getIntrinsicHeight().toFloat()
        val viewWidth: Float = iv.getMeasuredWidth().toFloat()
        val viewHeight: Float = iv.getMeasuredHeight().toFloat()
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
                rotate_btn.setEnabled(false)
            }

            override fun onAnimationEnd(animation: Animator) {
                imageRotation = newRotation % 360
                rotate_btn.setEnabled(true)
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
            iv.getLayoutParams().height = animatedHeight
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
                -(drawableWidth - iv.getMeasuredWidth()) / 2,
                -(drawableHeight - iv.getMeasuredHeight()) / 2
            )
            iv.setImageMatrix(matrix)
            iv.requestLayout()
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

}