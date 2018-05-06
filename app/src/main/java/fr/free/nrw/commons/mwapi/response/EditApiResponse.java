package fr.free.nrw.commons.mwapi.response;

import com.google.gson.annotations.SerializedName;

public class EditApiResponse {
    @SerializedName("edit")
    public EditResponse edit;

    public class EditResponse {
        @SerializedName("result")
        public String result;
    }
}
