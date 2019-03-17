package fr.free.nrw.commons.mwapi.model;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.mwapi.MwResponse;

import java.util.Map;

public class MwQueryResponse extends MwResponse {

    @SuppressWarnings("unused") @SerializedName("batchcomplete") private boolean batchComplete;

    @SuppressWarnings("unused") @SerializedName("continue") @Nullable
    private Map<String, String> continuation;

    @Nullable private MwQueryResult query;

    public boolean batchComplete() {
        return batchComplete;
    }

    @Nullable public Map<String, String> continuation() {
        return continuation;
    }

    @Nullable public MwQueryResult query() {
        return query;
    }

    public boolean success() {
        return query != null;
    }

    @VisibleForTesting
    protected void setQuery(@Nullable MwQueryResult query) {
        this.query = query;
    }
}