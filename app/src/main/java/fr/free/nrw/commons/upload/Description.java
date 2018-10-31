package fr.free.nrw.commons.upload;

import android.text.TextUtils;

class Description {

    private String languageId;
    private String languageDisplayText;
    private String descriptionText;
    private boolean set;
    private int selectedLanguageIndex = -1;

    /**
     * Gets language ID
     * @return String
     */
    public String getLanguageId() {
        return languageId;
    }

    /**
     * Sets language ID
     * @param languageId
     */
    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public String getLanguageDisplayText() {
        return languageDisplayText;
    }

    /**
     * Sets language Display Text
     * @param languageDisplayText
     */
    public void setLanguageDisplayText(String languageDisplayText) {
        this.languageDisplayText = languageDisplayText;
    }

    /**
     * Gets description text
     * @return
     */
    public String getDescriptionText() {
        return descriptionText;
    }

    /**
     * Sets description text
     * @param descriptionText
     */
    public void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;

        if (!TextUtils.isEmpty(descriptionText)) {
            set = true;
        }
    }

    /**
     * Checks if set
     * @return true if set
     */
    public boolean isSet() {
        return set;
    }

    /**
     * Sets set
     * @param set
     */
    public void setSet(boolean set) {
        this.set = set;
    }

    /**
     * Get selected language index
     * @return int
     */
    public int getSelectedLanguageIndex() {
        return selectedLanguageIndex;
    }

    /**
     * Sets selected language index
     * @param selectedLanguageIndex
     */
    public void setSelectedLanguageIndex(int selectedLanguageIndex) {
        this.selectedLanguageIndex = selectedLanguageIndex;
    }
}
