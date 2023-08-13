package fr.free.nrw.commons.edit

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
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


class EditActivity : AppCompatActivity() {
    var imageUri = ""
    lateinit var vm: EditViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)
        supportActionBar?.title = "Your Activity Title"; // for set actionbar title

        val intent = intent
        imageUri = intent.getStringExtra("image") ?: ""
        vm = ViewModelProvider(this).get(EditViewModel::class.java)

        init()


    }

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

    fun getRotatedImage() {

        val filePath = imageUri.toUri().path
        val file = filePath?.let { File(it) }
        val rotatedImage = file?.let { vm.rotateImage(imageRotation, it) }
        val resultIntent = Intent()
        resultIntent.putExtra("editedImageFilePath",rotatedImage?.toUri()?.path ?: "Error" );
        setResult(RESULT_OK, resultIntent);
        finish();

    }

}