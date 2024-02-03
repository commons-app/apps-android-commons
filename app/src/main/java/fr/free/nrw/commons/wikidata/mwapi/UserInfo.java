package fr.free.nrw.commons.wikidata.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;


public class UserInfo {
    @NonNull private String name;
    @NonNull private int id;

    //Block information
    private int blockid;
    private String blockedby;
    private int blockedbyid;
    private String blockreason;
    private String blocktimestamp;
    private String blockexpiry;

    // Object type is any JSON type.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Nullable private Map<String, ?> options;

    public int id() {
        return id;
    }

    @NonNull
    public String blockexpiry() {
        if (blockexpiry != null)
            return blockexpiry;
        else return "";
    }
}
