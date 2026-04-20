package fr.free.nrw.commons.edit

import android.graphics.Rect
import android.mediautil.image.jpeg.LLJTran
import android.mediautil.image.jpeg.LLJTranException
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Implementation of the TransformImage interface for image rotation and crop operations.
 *
 * This class provides an implementation for the TransformImage interface, exposing functions
 * for rotating and cropping images using the LLJTran library for lossless JPEG transforms.
 */
class TransformImageImpl : TransformImage {
    /**
     * Rotates the specified image file by the given degree.
     *
     * @param imageFile The File representing the image to be rotated.
     * @param degree The degree by which to rotate the image.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    override fun rotateImage(
        imageFile: File,
        degree: Int,
        savePath: File
    ): File? {
        Timber.tag("Trying to rotate image").d("Starting")

        val normalizedDegree = ((degree % 360) + 360) % 360
        val rotationOp =
            when (normalizedDegree) {
                0 -> null
                90 -> LLJTran.ROT_90
                180 -> LLJTran.ROT_180
                270 -> LLJTran.ROT_270
                else -> {
                    Timber.w("Unsupported rotation degree: $degree")
                    return null
                }
            }

        val imagePath = System.currentTimeMillis()
        val output = File(savePath, "rotated_$imagePath.jpg")

        val rotated =
            try {
                if (rotationOp == null) {
                    imageFile.copyTo(output, overwrite = true)
                    // Keep save behavior consistent with absolute rotation=0 target.
                    // If source had non-normal EXIF orientation, normalize it here.
                    forceExifOrientationNormal(output)
                } else if (shouldRotateLosslessly(imageFile, rotationOp)) {
                    rotateLosslessly(imageFile, output, rotationOp)
                    forceExifOrientationNormal(output)
                } else {
                    rotateWithExifOrientationOnly(imageFile, output, normalizedDegree)
                }
                true
            } catch (e: LLJTranException) {
                Timber.tag("Error").d(e)
                return null
            } catch (e: Exception) {
                Timber.tag("Error").d(e)
                return null
            }

        if (rotated) {
            Timber.tag("Done rotating image").d("Done")
            Timber.tag("Add").d(output.absolutePath)
        }
        return output
    }

    private fun shouldRotateLosslessly(imageFile: File, rotationOp: Int): Boolean {
        val lljTran = LLJTran(imageFile)
        try {
            lljTran.read(
                LLJTran.READ_ALL,
                false,
            ) // This could throw an LLJTranException. I am not catching it for now... Let's see.
            return lljTran.checkPerfect(rotationOp, null) == 0
        } finally {
            lljTran.freeMemory()
        }
    }

    private fun rotateLosslessly(imageFile: File, output: File, rotationOp: Int) {
        val lljTran = LLJTran(imageFile)
        try {
            lljTran.read(
                LLJTran.READ_ALL,
                false,
            ) // This could throw an LLJTranException. I am not catching it for now... Let's see.
            lljTran.transform(
                rotationOp,
                LLJTran.OPT_DEFAULTS or LLJTran.OPT_XFORM_ORIENTATION,
            )
            BufferedOutputStream(FileOutputStream(output)).use { writer ->
                lljTran.save(writer, LLJTran.OPT_WRITE_ALL)
            }
        } finally {
            lljTran.freeMemory()
        }
    }

    private fun rotateWithExifOrientationOnly(
        imageFile: File,
        output: File,
        normalizedDegree: Int,
    ) {
        imageFile.copyTo(output, overwrite = true)
        val exif = ExifInterface(output.absolutePath)
        val targetOrientation = exifOrientationForAbsoluteRotation(normalizedDegree)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, targetOrientation.toString())
        exif.saveAttributes()
    }

    private fun forceExifOrientationNormal(output: File) {
        try {
            val exif = ExifInterface(output.absolutePath)
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exif.saveAttributes()
        } catch (ex: Exception) {
            Timber.w(ex, "Failed to force EXIF orientation to tha Normal")
        }
    }

    private fun exifOrientationForAbsoluteRotation(rotationDegrees: Int): Int {
        return when (rotationDegrees) {
            0 -> ExifInterface.ORIENTATION_NORMAL
            90 -> ExifInterface.ORIENTATION_ROTATE_90
            180 -> ExifInterface.ORIENTATION_ROTATE_180
            270 -> ExifInterface.ORIENTATION_ROTATE_270
            else -> ExifInterface.ORIENTATION_UNDEFINED
        }
    }

    /**
     * Crops the specified image file using lossless JPEG cropping via LLJTran.
     *
     * Works around a porting bug in the Android LLJTran library (AndroidMediaUtil).
     * The original Java library used java.awt.Rectangle where `width` and `height`
     * are direct fields. The Android port replaced it with android.graphics.Rect,
     * but validateCropBounds still assigns width/height values into `right`/`bottom`:
     *   cropBounds.right = bounds.width();   // stores width, not right edge
     *   cropBounds.bottom = bounds.height();  // stores height, not bottom edge
     * Later, when the library calls cropBounds.width() (which computes right - left),
     * it gets (width - left) instead of width, producing wrong crop dimensions
     * whenever the crop origin is nonzero.
     *
     * See the buggy assignment in validateCropBounds:
     * https://github.com/bkhall/AndroidMediaUtil/blob/master/src/android/mediautil/image/jpeg/LLJTran.java
     *
     * The fix: pass "inflated" bounds where bounds.height() = cropHeight + alignedTop,
     * so that after the buggy subtraction: (cropHeight + alignedTop) - alignedTop = cropHeight.
     * This requires cropHeight + 2*alignedTop <= imageHeight to avoid clamping.
     * When that doesn't hold (crop is in the bottom/right half), we losslessly flip the
     * image first to bring the crop region near the origin, crop, then flip back.
     *
     * @param imageFile The File representing the image to be cropped.
     * @param left The left coordinate of the crop rectangle.
     * @param top The top coordinate of the crop rectangle.
     * @param width The width of the crop rectangle.
     * @param height The height of the crop rectangle.
     * @return The cropped image File, or null if the crop operation fails.
     */
    override fun cropImage(
        imageFile: File,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        savePath: File,
    ): File? {
        Timber.tag("Trying to crop image").d(
            "Starting crop: left=$left, top=$top, width=$width, height=$height"
        )

        val imagePath = System.currentTimeMillis()
        val output = File(savePath, "cropped_$imagePath.jpg")

        val cropped =
            try {
                val lljTran = LLJTran(imageFile)
                lljTran.read(LLJTran.READ_ALL, false)

                val mcuW = lljTran.mcuWidth
                val mcuH = lljTran.mcuHeight
                val imgW = lljTran.width
                val imgH = lljTran.height

                // MCU-align the crop origin (same logic as LLJTran.validateCropBounds)
                val alignedLeft = mcuAlign(left, mcuW, imgW)
                val alignedTop = mcuAlign(top, mcuH, imgH)

                val rawCropW = if (alignedLeft + width > imgW) imgW - alignedLeft else width
                val rawCropH = if (alignedTop + height > imgH) imgH - alignedTop else height

                // Round crop dimensions down to nearest MCU boundary to avoid
                // partial-block artifacts from the lossless JPEG crop workaround
                val cropW = ((rawCropW / mcuW) * mcuW).coerceAtLeast(mcuW)
                val cropH = ((rawCropH / mcuH) * mcuH).coerceAtLeast(mcuH)

                // Determine if flips are needed.
                // The inflate trick requires: cropDim + 2*alignedOrigin <= imageDim.
                // When that fails (crop is past the midpoint), we flip that axis first.
                val needFlipH = cropW + 2 * alignedLeft > imgW
                val needFlipV = cropH + 2 * alignedTop > imgH

                Timber.tag("Crop debug").d(
                    "img=${imgW}x${imgH}, mcu=${mcuW}x${mcuH}, " +
                        "aligned=($alignedLeft,$alignedTop), crop=${cropW}x${cropH}, " +
                        "flipH=$needFlipH, flipV=$needFlipV"
                )

                // Step 1: Pre-flip if needed (lossless, in-place DCT manipulation)
                val flipOpts = LLJTran.OPT_XFORM_ADJUST_EDGES
                if (needFlipV) lljTran.transform(LLJTran.FLIP_V, flipOpts)
                if (needFlipH) lljTran.transform(LLJTran.FLIP_H, flipOpts)

                // Step 2: Compute effective crop origin after potential flips
                val effLeft = if (needFlipH) imgW - alignedLeft - cropW else left
                val effTop = if (needFlipV) imgH - alignedTop - cropH else top

                // MCU-align the effective origin
                val effAlignedLeft = mcuAlign(effLeft, mcuW, imgW)
                val effAlignedTop = mcuAlign(effTop, mcuH, imgH)

                // Step 3: Build inflated bounds to compensate for the library bug.
                // validateCropBounds stores bounds.width() in cropBounds.right and
                // bounds.height() in cropBounds.bottom. Then adjustImageParameters
                // calls cropBounds.width() = right - left = bounds.width() - alignedLeft.
                // By inflating: bounds.width() = cropW + alignedLeft, the subtraction
                // yields: (cropW + alignedLeft) - alignedLeft = cropW. Correct!
                val inflatedW = cropW + effAlignedLeft
                val inflatedH = cropH + effAlignedTop
                val bounds = Rect(
                    effLeft, effTop,
                    effLeft + inflatedW, effTop + inflatedH
                )

                Timber.tag("Crop bounds").d(
                    "effective=($effLeft,$effTop), " +
                        "effAligned=($effAlignedLeft,$effAlignedTop), " +
                        "inflated bounds=$bounds"
                )

                // Step 4: Apply lossless crop (in-place)
                lljTran.transform(
                    LLJTran.CROP,
                    LLJTran.OPT_XFORM_ADJUST_EDGES,
                    bounds
                )

                // Step 5: Reverse flips to restore original orientation (in-place)
                if (needFlipH) lljTran.transform(LLJTran.FLIP_H, flipOpts)
                if (needFlipV) lljTran.transform(LLJTran.FLIP_V, flipOpts)

                // Step 6: Save
                BufferedOutputStream(FileOutputStream(output)).use { writer ->
                    lljTran.save(writer, LLJTran.OPT_WRITE_ALL)
                }
                lljTran.freeMemory()
                true
            } catch (e: LLJTranException) {
                Timber.tag("Error").d(e)
                return null
            } catch (e: Exception) {
                Timber.tag("Error").d(e)
                return null
            }

        if (cropped) {
            Timber.tag("Done cropping image").d("Done")
            Timber.tag("Add").d(output.absolutePath)
        }
        return output
    }

    /**
     * Aligns a coordinate to the nearest MCU boundary, matching LLJTran's
     * validateCropBounds rounding logic.
     */
    private fun mcuAlign(coord: Int, mcuSize: Int, imageSize: Int): Int {
        val rem = coord % mcuSize
        var aligned = coord - rem
        if (rem > mcuSize / 2 && aligned + mcuSize < imageSize) {
            aligned += mcuSize
        }
        return aligned
    }
}
