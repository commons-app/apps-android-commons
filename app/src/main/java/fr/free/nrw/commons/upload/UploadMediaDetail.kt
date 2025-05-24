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
     * The language code ie. "en" or "fr".
     * @property  languageCode The language code ie. "en" or "fr".
     */
    var languageCode: String? = null,
    /**
     * The description text for the item being uploaded.
     * @property  descriptionText The description text.
     */
    var descriptionText: String? = "",
    /**
     * The caption text for the item being uploaded.
     * @property  captionText The caption text.
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
     * @return The index of the selected language.
     * @param selectedLanguageIndex The index of the language selected.
     */
    var selectedLanguageIndex: Int = -1

    /**
     * Returns if the description was added manually (by the user, or programmatically).
     * @return True if the description was manually added.
     * @param manuallyAdded Sets to true if the description was manually added.
     */
    var isManuallyAdded: Boolean = false
}
