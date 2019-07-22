package fr.free.nrw.commons.media;
import java.util.HashMap;
import java.util.Map;;
import com.google.gson.annotations.SerializedName;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class MediaDetailResponse {

    @SerializedName("entities")
    private Map<String, CaptionMetadata> entities;
    @SerializedName("success")
    private Integer success;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     *
     */
    public MediaDetailResponse() {
    }

    /**
     *
     * @param success
     * @param entities
     */
    public MediaDetailResponse(Map<String, CaptionMetadata> entities, Integer success) {
        super();
        this.entities = entities;
        this.success = success;
    }

    public Map<String, CaptionMetadata> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, CaptionMetadata> entities) {
        this.entities = entities;
    }

    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(additionalProperties).append(success).append(entities).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof MediaDetailResponse) == false) {
            return false;
        }
        MediaDetailResponse rhs = ((MediaDetailResponse) other);
        return new EqualsBuilder().append(additionalProperties, rhs.additionalProperties).append(success, rhs.success).append(entities, rhs.entities).isEquals();
    }

}
