package fr.free.nrw.commons.upload

/**
 * Holds a description of an item being uploaded by [UploadActivity]
 */
class Description {
    /**
     * @return The language code ie. "en" or "fr"
     */
    /**
     * @param languageCode The language code ie. "en" or "fr"
     */
    var languageCode: String? = null
    var descriptionText: String? = null
    /**
     * @return the index of the  language selected in a spinner with [SpinnerLanguagesAdapter]
     */
    /**
     * @param selectedLanguageIndex the index of the language selected in a spinner with [SpinnerLanguagesAdapter]
     */
    var selectedLanguageIndex = -1
    /**
     * returns if the description was added manually (by the user, or we have added it programaticallly)
     * @return
     */
    /**
     * sets to true if the description was manually added by the user
     * @param manuallyAdded
     */
    var isManuallyAdded = false

    val isEmpty: Boolean
        get() = descriptionText == null || descriptionText!!.isEmpty()

    companion object {
        /**
         * Formats the list of descriptions into the format Commons requires for uploads.
         *
         * @param descriptions the list of descriptions, description is ignored if text is null.
         * @return a string with the pattern of {{en|1=descriptionText}}
         */
        @JvmStatic
        fun formatList(descriptions: List<Description>): String {
            val descListString = StringBuilder()
            for (description in descriptions) {
                if (!description.isEmpty) {
                    val individualDescription = String.format("{{%s|1=%s}}", description.languageCode,
                            description.descriptionText)
                    descListString.append(individualDescription)
                }
            }
            return descListString.toString()
        }
    }
}