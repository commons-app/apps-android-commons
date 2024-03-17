package fr.free.nrw.commons.wikidata.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import fr.free.nrw.commons.wikidata.model.gallery.ImageInfo;
import fr.free.nrw.commons.wikidata.model.BaseModel;

import java.util.Collections;
import java.util.List;

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
public class MwQueryPage extends BaseModel {
    private int pageid;
    private int index;
    @NonNull private String title;
    @NonNull private CategoryInfo categoryinfo;
    @Nullable private List<Revision> revisions;
    @SerializedName("fileusage") @Nullable private List<FileUsage> fileUsages;
    @SerializedName("globalusage") @Nullable private List<GlobalUsage> globalUsages;
    @Nullable private List<Coordinates> coordinates;
    @Nullable private List<Category> categories;
    @Nullable private Thumbnail thumbnail;
    @Nullable private String description;
    @SerializedName("imageinfo") @Nullable private List<ImageInfo> imageInfo;
    @Nullable private String redirectFrom;
    @Nullable private String convertedFrom;
    @Nullable private String convertedTo;

    @NonNull public String title() {
        return title;
    }

    @NonNull public CategoryInfo categoryInfo() {
        return categoryinfo;
    }

    public int index() {
        return index;
    }

    @Nullable public List<Revision> revisions() {
        return revisions;
    }

    @Nullable public List<Category> categories() {
        return categories;
    }

    @Nullable public List<Coordinates> coordinates() {
        // TODO: Handle null values in lists during deserialization, perhaps with a new
        // @RequiredElements annotation and corresponding TypeAdapter
        if (coordinates != null) {
            coordinates.removeAll(Collections.singleton(null));
        }
        return coordinates;
    }

    public int pageId() {
        return pageid;
    }

    @Nullable public String thumbUrl() {
        return thumbnail != null ? thumbnail.source() : null;
    }

    @Nullable public String description() {
        return description;
    }

    @Nullable public ImageInfo imageInfo() {
        return imageInfo != null ? imageInfo.get(0) : null;
    }

    public void redirectFrom(@Nullable String from) {
        redirectFrom = from;
    }

    public void convertedFrom(@Nullable String from) {
        convertedFrom = from;
    }

    public void convertedTo(@Nullable String to) {
        convertedTo = to;
    }

    public void appendTitleFragment(@Nullable String fragment) {
        title += "#" + fragment;
    }

    public boolean checkWhetherFileIsUsedInWikis() {
        if (globalUsages != null && globalUsages.size() > 0) {
            return true;
        }

        if (fileUsages == null || fileUsages.size() == 0) {
            return false;
        }

        final int totalCount = fileUsages.size();

        /* Ignore usage under https://commons.wikimedia.org/wiki/User:Didym/Mobile_upload/
           which has been a gallery of all of our uploads since 2014 */
        for (final FileUsage fileUsage : fileUsages) {
            if ( ! fileUsage.title().contains("User:Didym/Mobile upload")) {
                return true;
            }
        }

        return false;
    }

    public static class Revision {
        @SerializedName("revid") private long revisionId;
        private String user;
        @SerializedName("contentformat") @NonNull private String contentFormat;
        @SerializedName("contentmodel") @NonNull private String contentModel;
        @SerializedName("timestamp") @NonNull private String timeStamp;
        @NonNull private String content;

        @NonNull public String content() {
            return content;
        }

        @NonNull public String timeStamp() {
            return StringUtils.defaultString(timeStamp);
        }

        public long getRevisionId() {
            return revisionId;
        }

        @NonNull
        public String getUser() {
            return StringUtils.defaultString(user);
        }
    }

    public static class Coordinates {
         @Nullable private Double lat;
         @Nullable private Double lon;

        @Nullable public Double lat() {
            return lat;
        }
        @Nullable public Double lon() {
            return lon;
        }
    }

    public static class CategoryInfo {
         private boolean hidden;
         private int size;
         private int pages;
         private int files;
         private int subcats;
        public boolean isHidden() {
            return hidden;
        }
    }

    static class Thumbnail {
         private String source;
         private int width;
         private int height;
        String source() {
            return source;
        }
    }

    public static class GlobalUsage {
        @SerializedName("title") private String title;
        @SerializedName("wiki")private String wiki;
        @SerializedName("url") private String url;

        public String getTitle() {
            return title;
        }

        public String getWiki() {
            return wiki;
        }

        public String getUrl() {
            return url;
        }
    }

    public static class FileUsage {
        @SerializedName("pageid") private int pageid;
        @SerializedName("ns") private int ns;
        @SerializedName("title") private String title;

        public int pageId() {
            return pageid;
        }

        public int ns() {
            return ns;
        }

        public String title() {
            return title;
        }
    }

    public static class Category {
         private int ns;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String title;
         private boolean hidden;

        public int ns() {
            return ns;
        }

        @NonNull public String title() {
            return StringUtils.defaultString(title);
        }

        public boolean hidden() {
            return hidden;
        }
    }
}
