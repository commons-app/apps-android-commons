package fr.free.nrw.commons.customselector.database

import androidx.room.*

/**
 * Entity class for Not For Upload status.
 */
@Entity(tableName = "not_for_upload_table")
data class NotForUploadStatus(

    /**
     * Original image sha1.
     */
    @PrimaryKey
    val imageSHA1 : String,

    /**
     * imageSHA1 query result from API.
     */
    var imageResult : Boolean
)
