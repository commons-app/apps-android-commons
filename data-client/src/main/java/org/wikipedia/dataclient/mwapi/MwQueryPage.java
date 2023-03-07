package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.gallery.VideoInfo;
import org.wikipedia.model.BaseModel;
import org.wikipedia.page.Namespace;

import java.util.Collections;
import java.util.List;

/**
 * A class representing a standard page object as returned by the MediaWiki API.
 */
public class MwQueryPage extends BaseModel {
    @SuppressWarnings("unused") private int pageid;
    @SuppressWarnings("unused") private int ns;
    @SuppressWarnings("unused") private int index;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
    @SuppressWarnings("unused,NullableProblems") @NonNull private CategoryInfo categoryinfo;
    @SuppressWarnings("unused") @Nullable private List<LangLink> langlinks;
    @SuppressWarnings("unused") @Nullable private List<Revision> revisions;
    @SuppressWarnings("unused") @SerializedName("fileusage") @Nullable private List<FileUsage> fileUsages;
    @SuppressWarnings("unused") @SerializedName("globalusage") @Nullable private List<GlobalUsage> globalUsages;
    @SuppressWarnings("unused") @Nullable private List<Coordinates> coordinates;
    @SuppressWarnings("unused") @Nullable private List<Category> categories;
    @SuppressWarnings("unused") @Nullable private PageProps pageprops;
    @SuppressWarnings("unused") @Nullable private PageTerms terms;
    @SuppressWarnings("unused") @Nullable private String extract;
    @SuppressWarnings("unused") @Nullable private Thumbnail thumbnail;
    @SuppressWarnings("unused") @Nullable private String description;
    @SuppressWarnings("unused") @SerializedName("descriptionsource") @Nullable private String descriptionSource;
    @SuppressWarnings("unused") @SerializedName("imageinfo") @Nullable private List<ImageInfo> imageInfo;
    @SuppressWarnings("unused") @SerializedName("videoinfo") @Nullable private List<VideoInfo> videoInfo;
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

    @NonNull public Namespace namespace() {
        return Namespace.of(ns);
    }

    @Nullable public List<LangLink> langLinks() {
        return langlinks;
    }

    @Nullable public List<Revision> revisions() {
        return revisions;
    }

    @Nullable public List<Category> categories() {
        return categories;
    }

    @Nullable public List<GlobalUsage> globalUsages() {
        return globalUsages;
    }

    @Nullable public List<FileUsage> fileUsages() {
        return fileUsages;
    }

    @Nullable public List<Coordinates> coordinates() {
        // TODO: Handle null values in lists during deserialization, perhaps with a new
        // @RequiredElements annotation and corresponding TypeAdapter
        if (coordinates != null) {
            coordinates.removeAll(Collections.singleton(null));
        }
        return coordinates;
    }

    public List<String> labels() {
        return terms != null && terms.label != null ? terms.label : Collections.emptyList();
    }

    public int pageId() {
        return pageid;
    }

    @Nullable public PageProps pageProps() {
        return pageprops;
    }

    @Nullable public String extract() {
        return extract;
    }

    @Nullable public String thumbUrl() {
        return thumbnail != null ? thumbnail.source() : null;
    }

    @Nullable public String description() {
        return description;
    }

    @Nullable
    public String descriptionSource() {
        return descriptionSource;
    }

    @Nullable public ImageInfo imageInfo() {
        return imageInfo != null ? imageInfo.get(0) : null;
    }

    @Nullable public VideoInfo videoInfo() {
        return videoInfo != null ? videoInfo.get(0) : null;
    }

    @Nullable public String redirectFrom() {
        return redirectFrom;
    }

    public void redirectFrom(@Nullable String from) {
        redirectFrom = from;
    }

    @Nullable public String convertedFrom() {
        return convertedFrom;
    }

    public void convertedFrom(@Nullable String from) {
        convertedFrom = from;
    }

    @Nullable public String convertedTo() {
        return convertedTo;
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
        @SuppressWarnings("unused,NullableProblems") @SerializedName("contentformat") @NonNull private String contentFormat;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("contentmodel") @NonNull private String contentModel;
        @SuppressWarnings("unused,NullableProblems") @SerializedName("timestamp") @NonNull private String timeStamp;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String content;

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

    public static class LangLink {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String lang;
        @NonNull public String lang() {
            return lang;
        }
        @SuppressWarnings("unused,NullableProblems") @NonNull private String title;
        @NonNull public String title() {
            return title;
        }
    }

    public static class Coordinates {
        @SuppressWarnings("unused") @Nullable private Double lat;
        @SuppressWarnings("unused") @Nullable private Double lon;

        @Nullable public Double lat() {
            return lat;
        }
        @Nullable public Double lon() {
            return lon;
        }
    }

    public static class CategoryInfo {
        @SuppressWarnings("unused") private boolean hidden;
        @SuppressWarnings("unused") private int size;
        @SuppressWarnings("unused") private int pages;
        @SuppressWarnings("unused") private int files;
        @SuppressWarnings("unused") private int subcats;
        public boolean isHidden() {
            return hidden;
        }
    }

    static class Thumbnail {
        @SuppressWarnings("unused") private String source;
        @SuppressWarnings("unused") private int width;
        @SuppressWarnings("unused") private int height;
        String source() {
            return source;
        }
    }

    public static class PageProps {
        @SuppressWarnings("unused") @SerializedName("wikibase_item") @Nullable private String wikiBaseItem;
        @SuppressWarnings("unused") @Nullable private String displaytitle;
        @SuppressWarnings("unused") @Nullable private String disambiguation;

        @Nullable public String getDisplayTitle() {
            return displaytitle;
        }

        @NonNull public String getWikiBaseItem() {
            return StringUtils.defaultString(wikiBaseItem);
        }

        public boolean isDisambiguation() {
            return disambiguation != null;
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
        @SuppressWarnings("unused") private int ns;
        @SuppressWarnings("unused,NullableProblems") @Nullable private String title;
        @SuppressWarnings("unused") private boolean hidden;

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

    public static class PageTerms {
        @SuppressWarnings("unused") private List<String> alias;
        @SuppressWarnings("unused") private List<String> label;
    }
}
