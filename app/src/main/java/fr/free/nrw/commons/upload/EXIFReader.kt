package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.TAG_DATETIME
import androidx.exifinterface.media.ExifInterface.TAG_MAKE
import fr.free.nrw.commons.utils.ImageUtils.FILE_NO_EXIF
import fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton

/**
 * We try to minimize uploads from the Commons app that might be copyright violations.
 * If an image does not have any Exif metadata, then it was likely downloaded from the internet,
 * and is probably not an original work by the user. We detect these kinds of images by looking
 * for the presence of some basic Exif metadata.
 */
@Singleton
class EXIFReader @Inject constructor() {
    fun processMetadata(path: String): Single<Int> = Single.just(
        try {
            if (ExifInterface(path).hasMakeOrDate) IMAGE_OK else FILE_NO_EXIF
        } catch (e: Exception) {
            FILE_NO_EXIF
        }
    )

    private val ExifInterface.hasMakeOrDate get() =
        getAttribute(TAG_MAKE) != null || getAttribute(TAG_DATETIME) != null
}

