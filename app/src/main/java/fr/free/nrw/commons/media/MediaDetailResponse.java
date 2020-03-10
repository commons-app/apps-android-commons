package fr.free.nrw.commons.media;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Model class for object while fetching structured data
 */
public class MediaDetailResponse {

    @SerializedName("entities")
    private Map<String, CommonsWikibaseItem> entities;
    @SerializedName("success")
    private Integer success;

    /**
     * No args constructor for use in serialization
     */
    public MediaDetailResponse() {
    }

    /**
     * @param success
     * @param entities
     */
    public MediaDetailResponse(Map<String, CommonsWikibaseItem> entities, Integer success) {
        super();
        this.entities = entities;
        this.success = success;
    }

    public Map<String, CommonsWikibaseItem> getEntities() {
        return entities;
    }


    public Integer getSuccess() {
        return success;
    }

    public void setSuccess(Integer success) {
        this.success = success;
    }

}
