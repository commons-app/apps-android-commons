package fr.free.nrw.commons.mwapi.model;

import androidx.annotation.NonNull;

/**
 * The API reported an error in the payload.
 */
public interface ServiceError {
    @NonNull
    String getTitle();

    @NonNull String getDetails();
}