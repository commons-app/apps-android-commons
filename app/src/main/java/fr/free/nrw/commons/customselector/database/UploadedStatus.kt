package fr.free.nrw.commons.customselector.database

import java.util.Date

/**
 * Entity class for Uploaded Status.
 */
data class UploadedStatus(
    /**
     * Original image sha1.
     */
    val imageSHA1: String,
    /**
     * Modified image sha1 (after exif changes).
     */
    val modifiedImageSHA1: String,
    /**
     * imageSHA1 query result from API.
     */
    var imageResult: Boolean,
    /**
     * modifiedImageSHA1 query result from API.
     */
    var modifiedImageResult: Boolean,
    /**
     * lastUpdated for data validation.
     */
    var lastUpdated: Date? = null,
)
