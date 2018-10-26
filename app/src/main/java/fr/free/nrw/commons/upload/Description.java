package fr.free.nrw.commons.upload;

import android.text.TextUtils;

class Description {

    private String languageId;
    private String languageDisplayText;
    private String descriptionText;
    private boolean set;
    private int selectedLanguageIndex = -1;

    public String getLanguageId() {
        return languageId;
    }

    public void setLanguageId(String languageId) {
        this.languageId = languageId;
    }

    public String getLanguageDisplayText() {
        return languageDisplayText;
    }

    public void setLanguageDisplayText(String languageDisplayText) {
        this.languageDisplayText = languageDisplayText;
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;

        if (!TextUtils.isEmpty(descriptionText)) {
            set = true;
        }
    }

    public boolean isSet() {
        return set;
    }

    public void setSet(boolean set) {
        this.set = set;
    }

    public int getSelectedLanguageIndex() {
        return selectedLanguageIndex;
    }

    public void setSelectedLanguageIndex(int selectedLanguageIndex) {
        this.selectedLanguageIndex = selectedLanguageIndex;
    }
}
