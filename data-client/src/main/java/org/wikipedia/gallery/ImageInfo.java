package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Gson POJO for a standard image info object as returned by the API ImageInfo module
 */
@SuppressWarnings("unused")
public class ImageInfo implements Serializable {
    private int size;
    private int width;
    private int height;
    @Nullable private String source;
    @SerializedName("thumburl") @Nullable private String thumbUrl;
    @SerializedName("thumbwidth") private int thumbWidth;
    @SerializedName("thumbheight") private int thumbHeight;
    @SerializedName("url") @Nullable private String originalUrl;
    @SerializedName("descriptionurl") @Nullable private String descriptionUrl;
    @SerializedName("descriptionshorturl") @Nullable private String descriptionShortUrl;
    @SerializedName("mime") @Nullable private String mimeType;
    @SerializedName("extmetadata")@Nullable private ExtMetadata metadata;
    @Nullable private String user;
    @Nullable private String timestamp;

    /**
     * Query width, default width parameter of the API query in pixels.
     */
    final private static int QUERY_WIDTH = 640;

    /**
     * Threshold height, the minimum height of the image in pixels.
     */
    final private static int THRESHOLD_HEIGHT = 220;

    @NonNull
    public String getSource() {
        return StringUtils.defaultString(source);
    }

    public void setSource(@Nullable String source) {
        this.source = source;
    }

    public int getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Get the thumbnail width.
     * @return
     */
    public int getThumbWidth() { return thumbWidth; }

    /**
     * Get the thumbnail height.
     * @return
     */
    public int getThumbHeight() { return thumbHeight; }

    @NonNull public String getMimeType() {
        return StringUtils.defaultString(mimeType, "*/*");
    }

    @NonNull public String getThumbUrl() {
        updateThumbUrl();
        return StringUtils.defaultString(thumbUrl);
    }

    @NonNull public String getOriginalUrl() {
        return StringUtils.defaultString(originalUrl);
    }

    @NonNull public String getUser() {
        return StringUtils.defaultString(user);
    }

    @NonNull public String getTimestamp() {
        return StringUtils.defaultString(timestamp);
    }

    @Nullable public ExtMetadata getMetadata() {
        return metadata;
    }

    /**
     * Updates the ThumbUrl if image dimensions are not sufficient.
     * Specifically, in panoramic images the height retrieved is less than required due to large width to height ratio,
     * so we update the thumb url keeping a minimum height threshold.
     */
    private void updateThumbUrl() {
        // If thumbHeight retrieved from API is less than THRESHOLD_HEIGHT
        if(getThumbHeight() < THRESHOLD_HEIGHT){
            // If thumbWidthRetrieved is same as queried width ( If not tells us that the image has no larger dimensions. )
            if(getThumbWidth() == QUERY_WIDTH){
                // Calculate new width depending on the aspect ratio.
                final int finalWidth = (int)(THRESHOLD_HEIGHT * getThumbWidth() * 1.0 / getThumbHeight());
                thumbHeight = THRESHOLD_HEIGHT;
                thumbWidth = finalWidth;
                final String toReplace = "/" + QUERY_WIDTH + "px";
                final int position = thumbUrl.lastIndexOf(toReplace);
                thumbUrl = (new StringBuilder(thumbUrl)).replace(position, position + toReplace.length(), "/" + thumbWidth + "px").toString();
            }
        }
    }

}
