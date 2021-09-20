package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class MwException extends RuntimeException {
    @SuppressWarnings("unused") @Nullable private final MwServiceError error;

    @SuppressWarnings("unused") @Nullable private final List<MwServiceError> errors;

    public MwException(@Nullable MwServiceError error,
        @Nullable final List<MwServiceError> errors) {
        this.error = error;
        this.errors = errors;
    }

    @NonNull
    public List<MwServiceError> getErrors() {
        return errors;
    }

    public String getErrorCode() {
        if(error!=null) {
            return error.getCode();
        }
        return errors != null ? errors.get(0).getCode() : null;
    }

    @Nullable public MwServiceError getError() {
        return error;
    }

    @Nullable
    public String getTitle() {
        if (error != null) {
            return error.getTitle();
        }
        return errors != null ? errors.get(0).getTitle() : null;
    }

    @Override
    @Nullable
    public String getMessage() {
        if (error != null) {
            return error.getDetails();
        }
        return errors != null ? errors.get(0).getDetails() : null;
    }
}
