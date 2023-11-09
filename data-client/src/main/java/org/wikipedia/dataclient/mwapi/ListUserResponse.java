package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public class ListUserResponse {
    @SerializedName("name") @Nullable private String name;
    private long userid;
    @Nullable private List<String> groups;

    @Nullable public String name() {
        return name;
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }
}
