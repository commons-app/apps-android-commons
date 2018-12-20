package fr.free.nrw.commons.upload;

import java.util.List;

/**
 * Holds a description of an item being uploaded by {@link UploadActivity}
 */
class Description {

    private String languageCode;
    private String descriptionText;
    private int selectedLanguageIndex = -1;

    /**
     * @return The language code ie. "en" or "fr"
     */
    String getLanguageCode() {
        return languageCode;
    }

    /**
     * @param languageCode The language code ie. "en" or "fr"
     */
    void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    String getDescriptionText() {
        return descriptionText;
    }

    void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;
    }

    /**
     * @return the index of the  language selected in a spinner with {@link SpinnerLanguagesAdapter}
     */
    int getSelectedLanguageIndex() {
        return selectedLanguageIndex;
    }

    /**
     * @param selectedLanguageIndex the index of the language selected in a spinner with {@link SpinnerLanguagesAdapter}
     */
    void setSelectedLanguageIndex(int selectedLanguageIndex) {
        this.selectedLanguageIndex = selectedLanguageIndex;
    }


    /**
     * Formats the list of descriptions into the format Commons requires for uploads.
     *
     * @param descriptions the list of descriptions, description is ignored if text is null.
     * @return a string with the pattern of {{en|1=descriptionText}}
     */
    static String formatList(List<Description> descriptions) {
        StringBuilder descListString = new StringBuilder();
        for (Description description : descriptions) {
            if (!description.isEmpty()) {
                String individualDescription = String.format("{{%s|1=%s}}", description.getLanguageCode(),
                        description.getDescriptionText());
                descListString.append(individualDescription);
            }
        }
        return descListString.toString();
    }

    public boolean isEmpty() {
        return descriptionText == null || descriptionText.isEmpty();
    }
}
