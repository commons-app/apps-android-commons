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
     * queryWidth, Width parameter of API query in px.
     */
    final private int queryWidth = 640;


    /**
     * thresholdHeight, The minimum height of the image in px.
     */
    final private static int thresholdHeight = 220;

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
        // If thumbHeight retrieved from API is less than thresholdHeight
        if(getThumbHeight() < thresholdHeight){
            // If thumbWidthRetrieved is same as queried width ( If not tells us that the image has no larger dimensions. )
            if(getThumbWidth() == queryWidth){
                // Calculate new Width depending on the aspect ratio.
                final int finalWidth = (int)(thresholdHeight * getThumbWidth() * 1.0 / getThumbHeight());
                thumbHeight = thresholdHeight;
                thumbWidth = finalWidth;
                final String toReplace = "/" + queryWidth + "px";
                final int position = thumbUrl.lastIndexOf(toReplace);
                thumbUrl = (new StringBuilder(thumbUrl)).replace(position, position + toReplace.length(), "/" + thumbWidth + "px").toString();
            }
        }
    }

}
