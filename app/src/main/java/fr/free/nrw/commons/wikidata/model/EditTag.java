package fr.free.nrw.commons.wikidata.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Tag class used when adding wikidata edit tag
 */
public class EditTag {

    @SerializedName("revid")
    @Expose
    private Integer revid;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("actionlogid")
    @Expose
    private Integer actionlogid;
    @SerializedName("added")
    @Expose
    private List<String> added;
    @SerializedName("removed")
    @Expose
    private List<Object> removed;

    public EditTag(Integer revid, String status, Integer actionlogid, List<String> added, List<Object> removed) {
        this.revid = revid;
        this.status = status;
        this.actionlogid = actionlogid;
        this.added = added;
        this.removed = removed;
    }

    public Integer getRevid() {
        return revid;
    }

    public String getStatus() {
        return status;
    }

    public Integer getActionlogid() {
        return actionlogid;
    }

    public List<String> getAdded() {
        return added;
    }

    public List<Object> getRemoved() {
        return removed;
    }
}