package fr.free.nrw.commons.media;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class CaptionObject {

    @SerializedName("language")
    private String language;
    @SerializedName("value")
    private String value;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     *
     */
    public CaptionObject() {
    }

    /**
     *
     * @param value
     * @param language
     */
    public CaptionObject(String language, String value) {
        super();
        this.language = language;
        this.value = value;
    }

    @SerializedName("language")
    public String getLanguage() {
        return language;
    }

    @SerializedName("value")
    public String getValue() {
        return value;
    }
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(value).append(language).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CaptionObject) == false) {
            return false;
        }
        CaptionObject rhs = ((CaptionObject) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(value, rhs.value).append(language, rhs.language).isEquals();
    }
}
