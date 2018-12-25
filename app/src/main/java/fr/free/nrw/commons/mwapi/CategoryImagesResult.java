package fr.free.nrw.commons.mwapi;

import java.util.List;
import java.util.Map;

import fr.free.nrw.commons.Media;

public class CategoryImagesResult {
    private List<Media> mediaList;
    private Map<String, String> queryContinueParam;

    CategoryImagesResult(List<Media> mediaList, Map<String, String> queryContinueParam) {
        this.mediaList = mediaList;
        this.queryContinueParam = queryContinueParam;
    }

    public List<Media> getMediaList() {
        return mediaList;
    }

    public Map<String, String> getQueryContinueParam() {
        return queryContinueParam;
    }
}
