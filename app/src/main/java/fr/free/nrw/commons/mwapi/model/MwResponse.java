package fr.free.nrw.commons.mwapi.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import fr.free.nrw.commons.json.PostProcessingTypeAdapter;

public abstract class MwResponse implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings("unused")
    @Nullable
    private MwServiceError error;

    @SuppressWarnings("unused")
    @Nullable
    private Map<String, Warning> warnings;

    @SuppressWarnings("unused,NullableProblems")
    @SerializedName("servedby")
    @NonNull
    private String servedBy;

    @Override
    public void postProcess() {
        if (error != null) {
            throw new MwException(error);
        }
    }

    private class Warning {
        @SuppressWarnings("unused,NullableProblems")
        @NonNull
        private String warnings;
    }
}