package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.Service;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.StringUtil;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class GalleryItem implements Serializable {
    public static final int PREFERRED_GALLERY_IMAGE_SIZE = 1280;

    @SerializedName("section_id") private int sectionId;
    @SuppressWarnings("NullableProblems") @NonNull private String type;
    @Nullable @SerializedName("audio_type") private String audioType;
    @Nullable private TextInfo caption;
    private boolean showInGallery;
    @SuppressWarnings("NullableProblems") @NonNull private Titles titles;
    @Nullable private ImageInfo thumbnail;
    @Nullable private ImageInfo original;
    @Nullable private List<VideoInfo> sources;
    @Nullable @SerializedName("file_page") private String filePage;
    @Nullable private ArtistInfo artist;
    private double duration;
    @SuppressWarnings("NullableProblems") @NonNull private ImageLicense license;
    @Nullable private TextInfo description;
    @Nullable @SerializedName("wb_entity_id") private String entityId;
    @Nullable @SerializedName("structured") private StructuredData structuredData;

    public GalleryItem() {
    }

    public GalleryItem(@NonNull String title) {
        this.type = "*/*";
        this.titles = new Titles(title, StringUtil.addUnderscores(title), title);
        this.original = new ImageInfo();
        this.thumbnail = new ImageInfo();
        this.description = new TextInfo();
        this.license = new ImageLicense();
    }

    @NonNull
    public String getType() {
        return StringUtils.defaultString(type);
    }

    @NonNull
    public String getAudioType() {
        return StringUtils.defaultString(audioType);
    }

    @Nullable
    public TextInfo getCaption() {
        return caption;
    }

    public boolean isShowInGallery() {
        return showInGallery;
    }

    @NonNull
    public Titles getTitles() {
        return titles;
    }

    protected void setTitle(@NonNull String title) {
        titles = new Titles(title, StringUtil.addUnderscores(title), title);
    }

    @NonNull
    public ImageInfo getThumbnail() {
        if (thumbnail == null) {
            thumbnail = new ImageInfo();
        }
        return thumbnail;
    }

    @NonNull
    public String getThumbnailUrl() {
        return getThumbnail().getSource();
    }

    @NonNull
    public String getPreferredSizedImageUrl() {
        return ImageUrlUtil.getUrlForPreferredSize(getThumbnailUrl(), PREFERRED_GALLERY_IMAGE_SIZE);
    }

    @NonNull
    public ImageInfo getOriginal() {
        if (original == null) {
            original = new ImageInfo();
        }
        return original;
    }

    @Nullable
    public List<VideoInfo> getSources() {
        return sources;
    }

    @Nullable
    public VideoInfo getOriginalVideoSource() {
        // The getSources has different levels of source,
        // should have an option that allows user to chose which quality to play
        return sources == null || sources.size() == 0
                ? null : sources.get(sources.size() - 1);
    }

    public double getDuration() {
        return duration;
    }

    @NonNull
    public String getFilePage() {
        // return the base url of Wiki Commons for WikiSite() if the file_page is null.
        return filePage == null ? Service.COMMONS_URL : StringUtils.defaultString(filePage);
    }

    public void setFilePage(@NonNull String filePage) {
        this.filePage = filePage;
    }

    @Nullable
    public ArtistInfo getArtist() {
        return artist;
    }

    public void setArtist(@Nullable ArtistInfo artist) {
        this.artist = artist;
    }

    @NonNull
    public ImageLicense getLicense() {
        return license;
    }

    public void setLicense(@NonNull ImageLicense license) {
        this.license = license;
    }

    @NonNull
    public TextInfo getDescription() {
        if (description == null) {
            description = new TextInfo();
        }
        return description;
    }

    @NonNull
    public Map<String, String> getStructuredCaptions() {
        return (structuredData != null && structuredData.captions != null) ? structuredData.captions : Collections.emptyMap();
    }

    public static class Titles implements Serializable {
        @Nullable private String canonical;
        @Nullable private String normalized;
        @Nullable private String display;

        Titles(@NonNull String display, @NonNull String canonical, @NonNull String normalized) {
            this.display = display;
            this.canonical = canonical;
            this.normalized = normalized;
        }

        @NonNull
        public String getCanonical() {
            return StringUtils.defaultString(canonical);
        }

        @NonNull
        public String getNormalized() {
            return StringUtils.defaultString(normalized);
        }

        @NonNull
        public String getDisplay() {
            return StringUtils.defaultString(display);
        }
    }

    public static class StructuredData implements Serializable {
        @Nullable private HashMap<String, String> captions;
    }
}
