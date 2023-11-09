package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.model.BaseModel;

/**
 * Gson POJO for a MediaWiki API error.
 */
public class MwServiceError extends BaseModel {

     @Nullable private String code;
     @Nullable private String text;

    @NonNull public String getTitle() {
        return StringUtils.defaultString(code);
    }

    @NonNull public String getDetails() {
        return StringUtils.defaultString(text);
    }

    @Nullable
    public String getCode() {
        return code;
    }
}
