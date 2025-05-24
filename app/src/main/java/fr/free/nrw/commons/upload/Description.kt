package fr.free.nrw.commons.upload

/**
 * Holds a description of an item being uploaded by [UploadActivity]
 */
class Description {
    /**
     * The language code, e.g., "en" or "fr".
     * @property  languageCode The language code.
     */
    var languageCode: String? = null

    /**
     * The description text for the item being uploaded.
     * @property  descriptionText The description text.
     */
    var descriptionText: String? = null

    /**
     * The index of the language selected in a spinner with [SpinnerLanguagesAdapter].
     * @property  selectedLanguageIndex The index of the selected language.
     */
    var selectedLanguageIndex = -1

    /**
     * Indicates if the description was added manually (by the user or programmatically).
     */
    var isManuallyAdded = false
    /**
     * Returns true if the description text is null or empty.
     */
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
                    val individualDescription =
                        String.format(
                            "{{%s|1=%s}}",
                            description.languageCode,
                            description.descriptionText,
                        )
                    descListString.append(individualDescription)
                }
            }
            return descListString.toString()
        }
    }
}
