package fr.free.nrw.commons.media;

import com.google.gson.annotations.SerializedName;

public class MwParseResult {
    @SuppressWarnings("unused") private int pageid;
    @SuppressWarnings("unused") private int index;
    private MwParseText text;

    public String text() {
        return text.text;
    }


    public class MwParseText{
        @SerializedName("*") private String text;
    }
}
