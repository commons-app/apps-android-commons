package fr.free.nrw.commons.mwapi.model;

import com.google.gson.annotations.SerializedName;

public class RecentChange {
    private final String type;
    private final String title;
    @SerializedName("old_revid")
    private final String oldRevisionId;

    public RecentChange(String type, String title, String oldRevisionId) {
        this.type = type;
        this.title = title;
        this.oldRevisionId = oldRevisionId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getOldRevisionId() {
        return oldRevisionId;
    }
}
