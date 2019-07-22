package fr.free.nrw.commons.media;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

public class Labels {

    @SerializedName("en")
    private CaptionObject captionObject;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     */
    public Labels() {
    }

    /**
     * @param captionObject
     */
    public Labels(CaptionObject captionObject) {
        super();
        this.captionObject = captionObject;
    }

    public CaptionObject getCaptionObject() {
        return captionObject;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(captionObject).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof Labels) == false) {
            return false;
        }
        Labels rhs = ((Labels) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(captionObject, rhs.captionObject).isEquals();
    }
}

