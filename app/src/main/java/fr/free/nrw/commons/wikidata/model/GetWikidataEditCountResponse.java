package fr.free.nrw.commons.wikidata.model;

import com.google.gson.annotations.SerializedName;

public class GetWikidataEditCountResponse {
    @SerializedName("edits")
    private final int wikidataEditCount;

    public GetWikidataEditCountResponse(int wikidataEditCount) {
        this.wikidataEditCount = wikidataEditCount;
    }

    public int getWikidataEditCount() {
        return wikidataEditCount;
    }
}
