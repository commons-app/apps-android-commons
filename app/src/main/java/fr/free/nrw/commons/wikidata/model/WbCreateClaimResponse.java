package fr.free.nrw.commons.wikidata.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Wikidata create claim response model class
 */
public class WbCreateClaimResponse {

    @SerializedName("pageinfo")
    @Expose
    private PageInfo pageinfo;
    @SerializedName("success")
    @Expose
    private Integer success;

    public WbCreateClaimResponse(PageInfo pageinfo, Integer success) {
        this.pageinfo = pageinfo;
        this.success = success;
    }

    public PageInfo getPageinfo() {
        return pageinfo;
    }

    public Integer getSuccess() {
        return success;
    }
}