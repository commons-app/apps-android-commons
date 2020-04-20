package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class UserInfo {
    @NonNull
    private String name;
    @NonNull
    private int id;

    //Block information
    private int blockid;
    private String blockedby;
    private int blockedbyid;
    private String blockreason;
    private String blocktimestamp;
    private String blockexpiry;

    // Object type is any JSON type.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @Nullable
    private Map<String, ?> options;

    public int id() {
        return id;
    }

    @NonNull
    public Map<String, String> userjsOptions() {
        Map<String, String> map = new HashMap<>();
        if (options != null) {
            for (Map.Entry<String, ?> entry : options.entrySet()) {
                if (entry.getKey().startsWith("userjs-")) {
                    // T161866 entry.valueOf() should always return a String but doesn't
                    map.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                }
            }
        }
        return map;
    }

    @NonNull
    public int blockid() {
        return blockid;
    }

    @NonNull
    public String blockedby() {
        if (blockedby != null)
            return blockedby;
        else return "";
    }

    @NonNull
    public int blockedbyid() {
        return blockedbyid;
    }

    @NonNull
    public String blockreason() {
        if (blockreason != null)
            return blockreason;
        else return "";
    }

    @NonNull
    public String blocktimestamp() {
        if (blocktimestamp != null)
            return blocktimestamp;
        else return "";
    }

    @NonNull
    public String blockexpiry() {
        if (blockexpiry != null)
            return blockexpiry;
        else return "";
    }
}
