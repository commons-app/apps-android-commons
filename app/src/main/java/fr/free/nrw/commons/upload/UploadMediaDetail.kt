package fr.free.nrw.commons.upload

import android.os.Parcelable
import fr.free.nrw.commons.nearby.Place
import kotlinx.parcelize.Parcelize

/**
 * Holds a description of an item being uploaded by [UploadActivity]
 */
@Parcelize
data class UploadMediaDetail(
    /**
     * The language code, e.g., "en" or "fr".
     */
    var languageCode: String? = null,
    /**
     * The description text for the item being uploaded.
     */
    var descriptionText: String? = "",
    /**
     * The caption text for the item being uploaded.
     */
    var captionText: String = "",
) : Parcelable {
    fun javaCopy() = copy()

    constructor(place: Place?) : this(
        place?.language,
        place?.longDescription,
        place?.name ?: "",
    )

    /**
     * The index of the language selected in a spinner with [SpinnerLanguagesAdapter].
     */
    var selectedLanguageIndex: Int = -1

    /**
     * Indicates whether the description was added manually (by the user or programmatically).
     */
    var isManuallyAdded: Boolean = false
}
