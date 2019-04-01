package fr.free.nrw.commons.mwapi.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.media.model.ImageInfo;
import fr.free.nrw.commons.media.model.MwQueryPage;

public class MwQueryResult {
    @SuppressWarnings("unused")
    @Nullable
    private List<MwQueryPage> pages;
    private List<RecentChange> recentchanges;

    @NonNull
    public List<MwQueryPage> pages() {
        if (pages == null) {
            return new ArrayList<>();
        }
        return pages;
    }

    public List<RecentChange> getRecentchanges() {
        return recentchanges;
    }

    @Nullable
    public MwQueryPage firstPage() {
        return pages().get(0);
    }

    @NonNull
    public Map<String, ImageInfo> images() {
        Map<String, ImageInfo> result = new HashMap<>();
        if (pages != null) {
            for (MwQueryPage page : pages()) {
                if (page.imageInfo() != null) {
                    result.put(page.title(), page.imageInfo());
                }
            }
        }
        return result;
    }
}