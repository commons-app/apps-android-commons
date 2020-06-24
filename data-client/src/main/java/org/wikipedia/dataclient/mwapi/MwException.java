package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MwException extends RuntimeException {
    @SuppressWarnings("unused") @NonNull private final MwServiceError error;

    public MwException(@NonNull MwServiceError error) {
        this.error = error;
    }

    @NonNull public MwServiceError getError() {
        return error;
    }

    @Nullable public String getTitle() {
        return error.getTitle();
    }

    @Override @Nullable public String getMessage() {
        return error.getDetails();
    }
}
