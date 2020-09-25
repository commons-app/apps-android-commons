package fr.free.nrw.commons.contributions

import android.net.Uri
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.upload.WikidataPlace.Companion.from
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import kotlinx.android.parcel.Parcelize
import java.util.*

@Entity(tableName = "contribution")
@Parcelize
data class Contribution constructor(
    @Embedded(prefix = "media_") val media: Media,
    @PrimaryKey val pageId: String = media.pageId,
    var state: Int = 0,
    var transferred: Long = 0,
    val decimalCoords: String? = null,
    var dateCreatedSource: String? = null,
    var wikidataPlace: WikidataPlace? = null,
    var chunkInfo: ChunkInfo? = null,
    /**
     * @return array list of entityids for the depictions
     */
    /**
     * Each depiction loaded in depictions activity is associated with a wikidata entity id, this Id
     * is in turn used to upload depictions to wikibase
     */
    val depictedItems: List<DepictedItem> = ArrayList(),
    var mimeType: String? = null,
    val localUri: Uri? = null,
    var dataLength: Long = 0,
    var dateCreated: Date? = null,
    var dateModified: Date? = null,
    var hasInvalidLocation : Int =  0
) : Parcelable {

    fun completeWith(media: Media): Contribution {
        return copy(pageId = media.pageId, media = media, state = STATE_COMPLETED)
    }

    constructor(
        item: UploadItem,
        sessionManager: SessionManager,
        depictedItems: List<DepictedItem>,
        categories: List<String>
    ) : this(
        Media(
            formatCaptions(item.uploadMediaDetails),
            categories,
            item.fileName,
            formatDescriptions(item.uploadMediaDetails),
            sessionManager.authorName
        ),
        localUri = item.mediaUri,
        decimalCoords = item.gpsCoords.decimalCoords,
        dateCreatedSource = "",
        depictedItems = depictedItems,
        wikidataPlace = from(item.place)
    )

    /**
     * Set this true when ImageProcessor has said that the location is invalid
     * @param hasInvalidLocation
     */
    fun setHasInvalidLocation(hasInvalidLocation: Boolean) {
        this.hasInvalidLocation = if (hasInvalidLocation) 1 else 0
    }

    fun hasInvalidLocation(): Boolean {
        return hasInvalidLocation == 1
    }

    companion object {
        const val STATE_COMPLETED = -1
        const val STATE_FAILED = 1
        const val STATE_QUEUED = 2
        const val STATE_IN_PROGRESS = 3
        const val STATE_PAUSED = 4
        const val STATE_QUEUED_LIMITED_CONNECTION_MODE=5

        /**
         * Formatting captions to the Wikibase format for sending labels
         * @param uploadMediaDetails list of media Details
         */
        fun formatCaptions(uploadMediaDetails: List<UploadMediaDetail>) =
            uploadMediaDetails.associate { it.languageCode!! to it.captionText }
                .filter { it.value.isNotBlank() }

        /**
         * Formats the list of descriptions into the format Commons requires for uploads.
         *
         * @param descriptions the list of descriptions, description is ignored if text is null.
         * @return a string with the pattern of {{en|1=descriptionText}}
         */
        fun formatDescriptions(descriptions: List<UploadMediaDetail>) =
            descriptions.filter { it.descriptionText.isNotEmpty() }
                .joinToString { "{{${it.languageCode}|1=${it.descriptionText}}}" }
    }
}
