package fr.free.nrw.commons.media;

import com.google.gson.annotations.SerializedName;

/**
 * Model class for parsing Captions when fetching captions using filename in MediaClient
 */
public class Caption {

    /**
     * users language in which caption is written
     */
    @SerializedName("language")
    private String language;
    @SerializedName("value")
    private String value;

    /**
     * No args constructor for use in serialization
     */
    public Caption() {
    }

    /**
     * @param value
     * @param language
     */
    public Caption(String language, String value) {
        super();
        this.language = language;
        this.value = value;
    }

    @SerializedName("language")
    public String getLanguage() {
        return language;
    }

    @SerializedName("value")
    public String  getValue() {
        return value;
    }
}
