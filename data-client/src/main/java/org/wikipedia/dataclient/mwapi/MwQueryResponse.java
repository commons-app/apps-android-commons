package org.wikipedia.dataclient.mwapi;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class MwQueryResponse extends MwResponse {

    @SerializedName("continue") @Nullable private Map<String, String> continuation;

    @SerializedName("query") @Nullable private MwQueryResult query;

    @Nullable public Map<String, String> continuation() {
        return continuation;
    }

    @Nullable public MwQueryResult query() {
        return query;
    }

    public boolean success() {
        return query != null;
    }
}
