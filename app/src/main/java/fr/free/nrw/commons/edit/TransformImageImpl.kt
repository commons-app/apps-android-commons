package fr.free.nrw.commons.edit

import android.graphics.Rect
import android.mediautil.image.jpeg.LLJTran
import android.mediautil.image.jpeg.LLJTranException
import android.os.Environment
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
    ): File? {
        Timber.tag("Trying to rotate image").d("Starting")

        val path =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            )

        val imagePath = System.currentTimeMillis()
        val file: File = File(path, "$imagePath.jpg")

        val output = file

        val rotated =
            try {
                val lljTran = LLJTran(imageFile)
                lljTran.read(
                    LLJTran.READ_ALL,
                    false,
                ) // This could throw an LLJTranException. I am not catching it for now... Let's see.
                lljTran.transform(
                    when (degree) {
                        90 -> LLJTran.ROT_90
                        180 -> LLJTran.ROT_180
                        270 -> LLJTran.ROT_270
                        else -> {
                            LLJTran.ROT_90
                        }
                    },
                    LLJTran.OPT_DEFAULTS or LLJTran.OPT_XFORM_ORIENTATION,
                )
                BufferedOutputStream(FileOutputStream(output)).use { writer ->
                    lljTran.save(writer, LLJTran.OPT_WRITE_ALL)
                }
                lljTran.freeMemory()
                true
            } catch (e: LLJTranException) {
                Timber.tag("Error").d(e)
                return null
            }

        if (rotated) {
            Timber.tag("Done rotating image").d("Done")
            Timber.tag("Add").d(output.absolutePath)
        }
        return output
    }

    /**
     * Crops the specified image file using lossless JPEG cropping via LLJTran.
     *
     * Works around a porting bug in the Android LLJTran library where the internal
     * cropBounds Rect uses right/bottom to store width/height (java.awt.Rectangle
     * convention), but Rect.width()/height() compute right-left / bottom-top, giving
     * wrong results when the crop origin is nonzero.
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
    ): File? {
        Timber.tag("Trying to crop image").d(
            "Starting crop: left=$left, top=$top, width=$width, height=$height"
        )

        val path =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            )

        val imagePath = System.currentTimeMillis()
        val output = File(path, "$imagePath.jpg")

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

                val cropW = if (alignedLeft + width > imgW) imgW - alignedLeft else width
                val cropH = if (alignedTop + height > imgH) imgH - alignedTop else height

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
