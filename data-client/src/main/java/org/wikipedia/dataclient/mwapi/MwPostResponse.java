package org.wikipedia.dataclient.mwapi;

import androidx.annotation.Nullable;

public class MwPostResponse extends MwResponse {
    private int success;

    public boolean success(@Nullable String result) {
        return "success".equals(result);
    }

    public int getSuccessVal() {
        return success;
    }
}

