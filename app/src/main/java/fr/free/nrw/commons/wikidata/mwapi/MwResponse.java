package fr.free.nrw.commons.wikidata.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import fr.free.nrw.commons.wikidata.json.PostProcessingTypeAdapter;
import fr.free.nrw.commons.wikidata.model.BaseModel;

import java.util.List;

public abstract class MwResponse extends BaseModel implements PostProcessingTypeAdapter.PostProcessable {
    @SuppressWarnings({"unused"}) @Nullable private List<MwServiceError> errors;
    @SuppressWarnings("unused,NullableProblems") @SerializedName("servedby") @NonNull private String servedBy;

    @Override
    public void postProcess() {
        if (errors != null && !errors.isEmpty()) {
            throw new MwException(errors.get(0), errors);
        }
    }
}
