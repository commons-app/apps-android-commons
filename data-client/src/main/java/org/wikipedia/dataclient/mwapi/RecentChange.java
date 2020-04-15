package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class RecentChange {
    @Nullable private String type;
    @Nullable private String title;
    private long pageid;
    private long revid;
    @SerializedName("old_revid") private long oldRevisionId;
    @Nullable private String timestamp;

    @NonNull public String getType() {
        return StringUtils.defaultString(type);
    }

    @NonNull public String getTitle() {
        return StringUtils.defaultString(title);
    }

    public long getPageId() {
        return pageid;
    }

    public long getRevId() {
        return revid;
    }

    public long getOldRevisionId() {
        return oldRevisionId;
    }

    public String getTimestamp() {
        return StringUtils.defaultString(timestamp);
    }
}