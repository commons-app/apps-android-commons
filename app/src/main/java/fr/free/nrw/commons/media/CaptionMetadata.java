package fr.free.nrw.commons.media;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class CaptionMetadata {

    @SerializedName("type")
    private String type;
    @SerializedName("id")
    private String id;
    @SerializedName("labels")
    private Labels labels;
    @SerializedName("statements")
    private List<Object> statements = null;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     *
     */
    public CaptionMetadata() {
    }

    /**
     *
     * @param id
     * @param statements
     * @param labels
     * @param type
     */
    public CaptionMetadata(String type, String id, Labels labels, List<Object> statements) {
        super();
        this.type = type;
        this.id = id;
        this.labels = labels;
        this.statements = statements;
    }

    @SerializedName("type")
    public String getType() {
        return type;
    }

    @SerializedName("id")
    public String getId() {
        return id;
    }

    @SerializedName("labels")
    public Labels getLabels() {
        return labels;
    }

    @SerializedName("statements")
    public List<Object> getStatements() {
        return statements;
    }

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(id).append(additionalProperties).append(statements).append(labels).append(type).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof CaptionMetadata) == false) {
            return false;
        }
        CaptionMetadata rhs = ((CaptionMetadata) other);
        return new EqualsBuilder().append(id, rhs.id).append(additionalProperties, rhs.additionalProperties).append(statements, rhs.statements).append(labels, rhs.labels).append(type, rhs.type).isEquals();
    }


}
