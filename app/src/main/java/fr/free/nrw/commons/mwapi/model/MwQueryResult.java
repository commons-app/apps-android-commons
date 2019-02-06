package fr.free.nrw.commons.mwapi.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.free.nrw.commons.media.model.ImageInfo;
import fr.free.nrw.commons.media.model.MwQueryPage;

public class MwQueryResult {
    @SuppressWarnings("unused")
    @Nullable
    private HashMap<String, MwQueryPage> pages;

    @NonNull
    public List<MwQueryPage> pages() {
        if (pages == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(pages.values());
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