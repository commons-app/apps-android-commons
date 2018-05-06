package fr.free.nrw.commons.mwapi.response;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Map;

public class ParseApiResponse {
    @SerializedName("parse")
    public ParseResponse parse;

    public String parsedContent() {
        return parse != null ? parse.parsedContent() : "";
    }

    public class ParseResponse {
        @SerializedName("title")
        public String title;
        @SerializedName("pageid")
        public String pageId;
        @SerializedName("parsetree")
        public Map<String, String> parseTree;

        String parsedContent() {
            return parseTree != null && parseTree.size() > 0 ? new ArrayList<>(parseTree.values()).get(0) : "";
        }
    }
}
