package fr.free.nrw.commons.media;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.wikipedia.dataclient.mwapi.MwResponse;

public class MwParseResponse extends MwResponse {
    @Nullable
    private MwParseResult parse;

    @Nullable
    public MwParseResult parse() {
        return parse;
    }

    public boolean success() {
        return parse != null;
    }

    @VisibleForTesting
    protected void setParse(@Nullable MwParseResult parse) {
        this.parse = parse;
    }
}
