package fr.free.nrw.commons.upload

import fr.free.nrw.commons.nearby.Place
import java.util.*

/**
 * Holds a description of an item being uploaded by [UploadActivity]
 */
data class UploadMediaDetail constructor(
    /**
     * @return The language code ie. "en" or "fr"
     */
    /**
     * @param languageCode The language code ie. "en" or "fr"
     */
    var languageCode: String? = null,
    var descriptionText: String = "",
    var captionText: String = ""
) {
    fun javaCopy() = copy()

    constructor(place: Place) : this(
        Locale.getDefault().language,
        place.longDescription,
        place.name
    )
    /**
     * @return the index of the  language selected in a spinner with [SpinnerLanguagesAdapter]
     */
    /**
     * @param selectedLanguageIndex the index of the language selected in a spinner with [SpinnerLanguagesAdapter]
     */
    var selectedLanguageIndex: Int = -1
    /**
     * returns if the description was added manually (by the user, or we have added it programaticallly)
     * @return
     */
    /**
     * sets to true if the description was manually added by the user
     * @param manuallyAdded
     */
    var isManuallyAdded: Boolean = false

}
