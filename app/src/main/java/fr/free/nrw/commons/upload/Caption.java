package fr.free.nrw.commons.upload;

public class Caption {
    private String languageCode;
    private String captionText;
    private int selectedLanguageIndex = -1;
    private boolean isManuallyAdded=false;

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getCaptionText() {
        return captionText;
    }

    public void setCaptionText(String captionText) {
        this.captionText = captionText;
    }

    public int getSelectedLanguageIndex() {
        return selectedLanguageIndex;
    }

    public void setSelectedLanguageIndex(int selectedLanguageIndex) {
        this.selectedLanguageIndex = selectedLanguageIndex;
    }

    public boolean isManuallyAdded() {
        return isManuallyAdded;
    }

    public void setManuallyAdded(boolean manuallyAdded) {
        isManuallyAdded = manuallyAdded;
    }
}
