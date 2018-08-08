package fr.free.nrw.commons.upload;

import android.text.TextUtils;

import java.util.List;

class Description {

    private String languageCode;
    private String descriptionText;
    private boolean set;
    private int selectedLanguageIndex = -1;

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
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


    public static String formatList(List<Description> descriptions) {
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
