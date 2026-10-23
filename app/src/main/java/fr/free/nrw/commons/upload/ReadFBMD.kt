package fr.free.nrw.commons.upload

import fr.free.nrw.commons.utils.ImageUtils.FILE_FBMD
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.reactivex.Single
import timber.log.Timber
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * We want to discourage users from uploading images to Commons that were taken from Facebook. This
 * attempts to detect whether an image was downloaded from Facebook by heuristically searching for
 * metadata that is specific to images that come from Facebook.
 */
@Singleton
class ReadFBMD
    @Inject
    constructor() {
        fun processMetadata(path: String?): Single<Int> =
            Single.fromCallable {
                var result = IMAGE_OK
                try {
                    var psBlockOffset: Int
                    var fbmdOffset: Int

                    FileInputStream(path).use { fs ->
                        val bytes = ByteArray(4096)
                        fs.read(bytes)
                        with(String(bytes)) {
                            psBlockOffset = indexOf("8BIM")
                            fbmdOffset = indexOf("FBMD")
                        }
                    }

                    result = if (isFacebookMetadata(psBlockOffset, fbmdOffset)) FILE_FBMD else IMAGE_OK
                } catch (e: IOException) {
                    Timber.e(e)
                }
                return@fromCallable result
            }

        companion object {
            private const val MAX_FBMD_DISTANCE_FROM_PS_BLOCK = 0x80

            /**
             * Returns true if an FBMD marker was found shortly (less than 0x80 bytes) after
             * the Photoshop 8BIM block, which is how Facebook embeds its metadata
             */
            internal fun isFacebookMetadata(
                psBlockOffset: Int,
                fbmdOffset: Int,
            ): Boolean {
                val areMarkersFound = psBlockOffset > 0 && fbmdOffset > 0
                return areMarkersFound && fbmdOffset - psBlockOffset in 1 until MAX_FBMD_DISTANCE_FROM_PS_BLOCK
            }
        }
    }
