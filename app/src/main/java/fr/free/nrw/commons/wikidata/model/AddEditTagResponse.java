package fr.free.nrw.commons.wikidata.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response class for add edit tag
 */
public class AddEditTagResponse {

    @SerializedName("tag")
    @Expose
    private List<EditTag> tag = null;

    public List<EditTag> getTag() {
        return tag;
    }

    public void setTag(List<EditTag> tag) {
        this.tag = tag;
    }

}