package fr.free.nrw.commons.upload

import android.net.Uri
import fr.free.nrw.commons.Utils
import fr.free.nrw.commons.filepicker.MimeTypeMapWrapper.Companion.getExtensionFromMimeType
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ImageUtils
import io.reactivex.subjects.BehaviorSubject

class UploadItem(
    var mediaUri: Uri,
    val mimeType: String?,
    var gpsCoords: ImageCoordinates?,
    var place: Place,
    val createdTimestamp: Long,
    val createdTimestampSource: String?,
    /**
     * Uri of uploadItem
     * Uri points to image location or name, eg content://media/external/images/camera/10495 (Android 10)
     */
    var contentUri: Uri?,
    //according to EXIF data
    val fileCreatedDateString: String?
) {
    var imageQuality: Int = ImageUtils.IMAGE_WAIT
    var uploadMediaDetails: List<UploadMediaDetail> = listOf(UploadMediaDetail())
    var hasInvalidLocation = false
    var isWLMUpload = false
    var countryCode: String? = null

    /**
     * Choose a filename for the media. Currently, the caption is used as a filename. If several
     * languages have been entered, the first language is used.
     */
    val filename: String
        get() = Utils.fixExtension(
            uploadMediaDetails[0].captionText,
            getExtensionFromMimeType(mimeType)
        )

    fun hasInvalidLocation(): Boolean = hasInvalidLocation

    /**
     * Sets both the contentUri and mediaUri to the specified Uri.
     * This method allows you to assign the same Uri to both the contentUri and mediaUri
     * properties.
     *
     * @param uri The Uri to be set as both the contentUri and mediaUri.
     */
    fun setContentAndMediaUri(uri: Uri) {
        contentUri = uri
        mediaUri = uri
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UploadItem) {
            return false
        }
        return mediaUri.toString().contains((other).mediaUri.toString())
    }

    override fun hashCode(): Int {
        return mediaUri.hashCode()
    }
}
