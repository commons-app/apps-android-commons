package fr.free.nrw.commons.mwapi.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.json.PostProcessingTypeAdapter;

import java.util.Map;

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