package fr.free.nrw.commons.upload;

import android.util.Log;

import java.util.List;

import timber.log.Timber;

/**
 * Holds a description of an item being uploaded by {@link UploadActivity}
 */
public class UploadMediaDetail {

    private String languageCode;
    private String descriptionText;
    public String captionText;
    private int selectedLanguageIndex = -1;
    private boolean isManuallyAdded=false;

    /**
     * Formatting captions to the Wikibase format for sending labels
     * @param uploadMediaDetails list of media Details*/

    public static String formatCaptions(List<UploadMediaDetail> uploadMediaDetails) {
        StringBuilder captionListString = new StringBuilder();
        captionListString.append("{\\\"labels\\\":{");
        UploadMediaDetail uploadMediaDetail = uploadMediaDetails.get(0);
        String individualDescription = "\\\""+uploadMediaDetail.getLanguageCode()+"\\\":{\\\"language\\\":\\\""+uploadMediaDetail.getLanguageCode()+"\\\",\\\"value\\\":\\\""+uploadMediaDetail.getLanguageCode()+"-"+uploadMediaDetail.getCaptionText()+"\\\"}";
        captionListString.append(individualDescription);
        for (int i = 1;i<= uploadMediaDetails.size() - 1; i++) {
            uploadMediaDetail = uploadMediaDetails.get(i);
            individualDescription = ",\\\""+uploadMediaDetail.getLanguageCode()+"\\\":{\\\"language\\\":\\\""+uploadMediaDetail.getLanguageCode()+"\\\",\\\"value\\\":\\\""+uploadMediaDetail.getLanguageCode()+"-"+uploadMediaDetail.getCaptionText()+"\\\"}";
            captionListString.append(individualDescription);
        }
        Timber.e(captionListString.toString());
        return captionListString.toString();
    }

    public String getCaptionText() {
        return captionText;
    }

    public void setCaptionText(String captionText) {
        this.captionText = captionText;
    }

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
     * returns if the description was added manually (by the user, or we have added it programaticallly)
     * @return
     */
    public boolean isManuallyAdded() {
        return isManuallyAdded;
    }

    /**
     * sets to true if the description was manually added by the user
     * @param manuallyAdded
     */
    public void setManuallyAdded(boolean manuallyAdded) {
        isManuallyAdded = manuallyAdded;
    }

    /**
     * Formats the list of descriptions into the format Commons requires for uploads.
     *
     * @param descriptions the list of descriptions, description is ignored if text is null.
     * @return a string with the pattern of {{en|1=descriptionText}}
     */
    static String formatList(List<UploadMediaDetail> descriptions) {
        StringBuilder descListString = new StringBuilder();
        for (UploadMediaDetail description : descriptions) {
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
