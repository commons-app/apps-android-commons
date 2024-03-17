package fr.free.nrw.commons.wikidata.model.edit;

import androidx.annotation.Nullable;
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse;

public class Edit extends MwPostResponse {
    @Nullable private Result edit;

    @Nullable public Result edit() {
    return edit;
    }

    public class Result {
        @Nullable private String result;
        @Nullable private String code;
        @Nullable private String info;
        @Nullable private String warning;

        public boolean editSucceeded() {
            return "Success".equals(result);
        }

        @Nullable public String code() {
            return code;
        }

        @Nullable public String info() {
            return info;
        }

        @Nullable public String warning() {
            return warning;
        }

    }
}
