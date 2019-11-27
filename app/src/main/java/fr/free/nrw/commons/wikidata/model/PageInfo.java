package fr.free.nrw.commons.wikidata.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * PageInfo model class with last revision id of the edited Wikidata entity
 */
public class PageInfo {
    @SerializedName("lastrevid")
    @Expose
    private Long lastrevid;

    public PageInfo(Long lastrevid) {
        this.lastrevid = lastrevid;
    }

    public Long getLastrevid() {
        return lastrevid;
    }
}