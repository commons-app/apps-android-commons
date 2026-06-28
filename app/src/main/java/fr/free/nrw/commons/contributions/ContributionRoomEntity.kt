package fr.free.nrw.commons.contributions

import android.net.Uri
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import java.util.Date

/**
 * Room entity for the contribution table.
 */
@Entity(tableName = "contribution")
data class ContributionRoomEntity(
    @Embedded(prefix = "media_") val media: Media,
    @PrimaryKey val pageId: String,
    val state: Int,
    val transferred: Long,
    val decimalCoords: String?,
    val dateCreatedSource: String?,
    val wikidataPlace: WikidataPlace?,
    val chunkInfo: ChunkInfo?,
    val errorInfo: String?,
    val depictedItems: List<DepictedItem>,
    val mimeType: String?,
    val localUri: Uri?,
    val dataLength: Long,
    val dateCreated: Date?,
    val dateCreatedString: String?,
    val dateModified: Date?,
    val dateUploadStarted: Date?,
    val hasInvalidLocation: Int,
    val contentUri: Uri?,
    val countryCode: String?,
    val imageSHA1: String?,
    val retries: Int
)
