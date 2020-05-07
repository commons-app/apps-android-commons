package fr.free.nrw.commons;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.media.Depictions;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import fr.free.nrw.commons.utils.MediaDataExtractorUtil;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.gallery.ExtMetadata;
import org.wikipedia.gallery.ImageInfo;
import org.wikipedia.page.PageTitle;

@Entity
public class Media implements Parcelable {

    public static final Media EMPTY = new Media("");

    // Primary metadata fields
    @Nullable
    private Uri localUri;
    private String thumbUrl;
    private String imageUrl;
    private String filename;
    private String thumbnailTitle;
    /*
     * Captions are a feature part of Structured data. They are meant to store short, multilingual descriptions about files
     * This is a replacement of the previously used titles for images (titles were not multilingual)
     * Also now captions replace the previous convention of using title for filename
     */
    private String caption;
    private String description; // monolingual description on input...
    private String discussion;
    private long dataLength;
    private Date dateCreated;
    @Nullable private Date dateUploaded;
    private String license;
    private String licenseUrl;
    private String creator;
    /**
     * Wikibase Identifier associated with media files
     */
    @PrimaryKey
    @NonNull
    private String pageId;
    private List<String> categories; // as loaded at runtime?
    /**
     * Depicts is a feature part of Structured data. Multiple Depictions can be added for an image just like categories.
     * However unlike categories depictions is multi-lingual
     */
    private Depictions depictions;
    private boolean requestedDeletion;
    @Nullable private  LatLng coordinates;

    /**
     * Provides local constructor
     */
    public Media() {
        pageId = UUID.randomUUID().toString();
    }

    public static final Creator<Media> CREATOR = new Creator<Media>() {
        @Override
        public Media createFromParcel(final Parcel source) {
            return new Media(source);
        }

        @Override
        public Media[] newArray(final int size) {
            return new Media[size];
        }
    };

    /**
     * Provides a minimal constructor
     *
     * @param filename Media filename
     */
    public Media(final String filename) {
        this();
        this.filename = filename;
    }

    /**
     * Provide Media constructor
     * @param localUri Media URI
     * @param imageUrl Media image URL
     * @param filename Media filename
     * @param description Media description
     * @param dataLength Media date length
     * @param dateCreated Media creation date
     * @param dateUploaded Media date uploaded
     * @param creator Media creator
     */
    public Media(final Uri localUri, final String imageUrl, final String filename,
        final String description,
        final long dataLength, final Date dateCreated, final Date dateUploaded,
        final String creator) {
        this();
        this.localUri = localUri;
        thumbUrl = imageUrl;
        this.imageUrl = imageUrl;
        this.filename = filename;
        this.description = description;
        this.dataLength = dataLength;
        this.dateCreated = dateCreated;
        this.dateUploaded = dateUploaded;
        this.creator = creator;
    }

    public Media(final String pageId,
        final Uri localUri,
        final String thumbUrl,
        final String imageUrl,
        final String filename,
        final String description,
        final String discussion,
        final long dataLength,
        final Date dateCreated,
        final Date dateUploaded,
        final String license,
        final String licenseUrl,
        final String creator,
        final List<String> categories,
        final boolean requestedDeletion,
        final LatLng coordinates) {
        this.pageId = pageId;
        this.localUri = localUri;
        this.thumbUrl = thumbUrl;
        this.imageUrl = imageUrl;
        this.filename = filename;
        this.description = description;
        this.discussion = discussion;
        this.dataLength = dataLength;
        this.dateCreated = dateCreated;
        this.dateUploaded = dateUploaded;
        this.license = license;
        this.licenseUrl = licenseUrl;
        this.creator = creator;
        this.categories = categories;
        this.requestedDeletion = requestedDeletion;
        this.coordinates = coordinates;
    }

    public Media(final Uri localUri, final String filename,
        final String description, final String creator, final List<String> categories) {
        this(localUri,null, filename,
            description, -1, null, new Date(), creator);
        this.categories = categories;
    }

    public Media(final String title, final Date date, final String user) {
        this(null, null, title, "", -1, date, date, user);
    }

    protected Media(final Parcel in) {
        localUri = in.readParcelable(Uri.class.getClassLoader());
        thumbUrl = in.readString();
        imageUrl = in.readString();
        filename = in.readString();
        thumbnailTitle = in.readString();
        caption = in.readString();
        description = in.readString();
        discussion = in.readString();
        dataLength = in.readLong();
        final long tmpDateCreated = in.readLong();
        dateCreated = tmpDateCreated == -1 ? null : new Date(tmpDateCreated);
        final long tmpDateUploaded = in.readLong();
        dateUploaded = tmpDateUploaded == -1 ? null : new Date(tmpDateUploaded);
        license = in.readString();
        licenseUrl = in.readString();
        creator = in.readString();
        pageId = in.readString();
        final ArrayList<String> list = new ArrayList<>();
        in.readStringList(list);
        categories = list;
        in.readParcelable(Depictions.class.getClassLoader());
        requestedDeletion = in.readByte() != 0;
        coordinates = in.readParcelable(LatLng.class.getClassLoader());
    }

    /**
     * Creating Media object from MWQueryPage.
     * Earlier only basic details were set for the media object but going forward,
     * a full media object(with categories, descriptions, coordinates etc) can be constructed using this method
     *
     * @param page response from the API
     * @return Media object
     */
    @Nullable
    public static Media from(final MwQueryPage page) {
        final ImageInfo imageInfo = page.imageInfo();
        if (imageInfo == null) {
            return new Media(); // null is not allowed
        }
        final ExtMetadata metadata = imageInfo.getMetadata();
        if (metadata == null) {
            final Media media = new Media(null, imageInfo.getOriginalUrl(),
                    page.title(), "", 0, null, null, null);
            if (!StringUtils.isBlank(imageInfo.getThumbUrl())) {
                media.setThumbUrl(imageInfo.getThumbUrl());
            }
            return media;
        }

        final Media media = new Media(null,
                imageInfo.getOriginalUrl(),
                page.title(),
            "",
                0,
                safeParseDate(metadata.dateTime()),
                safeParseDate(metadata.dateTime()),
                getArtist(metadata)
        );

        if (!StringUtils.isBlank(imageInfo.getThumbUrl())) {
            media.setThumbUrl(imageInfo.getThumbUrl());
        }

        media.setPageId(String.valueOf(page.pageId()));

        String language = Locale.getDefault().getLanguage();
        if (StringUtils.isBlank(language)) {
            language = "default";
        }

        media.setDescription(metadata.imageDescription());
        media.setCategories(MediaDataExtractorUtil.extractCategoriesFromList(metadata.getCategories()));
        final String latitude = metadata.getGpsLatitude();
        final String longitude = metadata.getGpsLongitude();

        if (!StringUtils.isBlank(latitude) && !StringUtils.isBlank(longitude)) {
            final LatLng latLng = new LatLng(Double.parseDouble(latitude),
                Double.parseDouble(longitude), 0);
            media.setCoordinates(latLng);
        }

        media.setLicenseInformation(metadata.licenseShortName(), metadata.licenseUrl());
        return media;
    }

    /**
     * This method extracts the Commons Username from the artist HTML information
     * @param metadata
     * @return
     */
    private static String getArtist(final ExtMetadata metadata) {
        try {
            final String artistHtml = metadata.artist();
            return artistHtml.substring(artistHtml.indexOf("title=\""), artistHtml.indexOf("\">"))
                    .replace("title=\"User:", "");
        } catch (final Exception ex) {
            return "";
        }
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    /**
     * Gets media display title
     * @return Media title
     */
    @NonNull public String getDisplayTitle() {
        return filename != null ? getPageTitle().getDisplayTextWithoutNamespace().replaceFirst("[.][^.]+$", "") : "";
    }

    @Nullable
    private static Date safeParseDate(final String dateStr) {
        try {
            return CommonsDateUtil.getIso8601DateFormatShort().parse(dateStr);
        } catch (final ParseException e) {
            return null;
        }
    }

    /**
     * @return title to be shown on image thumbnail
     * If caption is available for the image then it returns caption else filename
     */
    public String getThumbnailTitle() {
        return thumbnailTitle != null? thumbnailTitle : getDisplayTitle();
    }

    /**
     * Gets file page title
     * @return New media page title
     */
    @NonNull public PageTitle getPageTitle() {
        return Utils.getPageTitle(getFilename());
    }

    /**
     * Gets local URI
     * @return Media local URI
     */
    public Uri getLocalUri() {
        return localUri;
    }

    /**
     * Gets image URL
     * can be null.
     * @return Image URL
     */
    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Gets the name of the file.
     * @return file name as a string
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return pageId for the current media object*/
    @NonNull
    public String getPageId() {
        return pageId;
    }

    /**
     *sets pageId for the current media object
     */
    public void setPageId(final String pageId) {
        this.pageId = pageId;
    }

    /**
     * Gets the file discussion as a string.
     * @return file discussion as a string
     */
    public String getDiscussion() {
        return discussion;
    }

    /**
     * Gets the file description.
     * @return file description as a string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Captions are a feature part of Structured data. They are meant to store short, multilingual descriptions about files
     * This is a replacement of the previously used titles for images (titles were not multilingual)
     * Also now captions replace the previous convention of using title for filename
     *
     * @return caption
     */
    public String getCaption() {
        return caption;
    }

    /**
     * @return depictions associated with the current media
     */
    public Depictions getDepiction() {
        return depictions;
    }

    /**
     * Sets the name of the file.
     * @param filename the new name of the file
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * Gets the dataLength of the file.
     * @return file dataLength as a long
     */
    public long getDataLength() {
        return dataLength;
    }

    /**
     * Sets the discussion of the file.
     * @param discussion
     */
    public void setDiscussion(final String discussion) {
        this.discussion = discussion;
    }

    /**
     * Gets the creation date of the file.
     * @return creation date as a Date
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * Sets the file description.
     * @param description the new description of the file
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets the upload date of the file.
     * Can be null.
     * @return upload date as a Date
     */
    public @Nullable
    Date getDateUploaded() {
        return dateUploaded;
    }

    /**
     * Gets the name of the creator of the file.
     * @return creator name as a String
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Sets the dataLength of the file.
     * @param dataLength as a long
     */
    public void setDataLength(final long dataLength) {
        this.dataLength = dataLength;
    }

    /**
     * Gets the license name of the file.
     * @return license as a String
     */
    public String getLicense() {
        return license;
    }

    /**
     * Set Caption(if available) as the thumbnail title of the image
     */
    public void setThumbnailTitle(final String title) {
        thumbnailTitle = title;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    /**
     * Sets the creator name of the file.
     * @param creator creator name as a string
     */
    public void setCreator(final String creator) {
        this.creator = creator;
    }

    /**
     * Gets the coordinates of where the file was created.
     * @return file coordinates as a LatLng
     */
    public @Nullable
    LatLng getCoordinates() {
        return coordinates;
    }

    public void setThumbUrl(final String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    /**
     * Gets the categories the file falls under.
     * @return file categories as an ArrayList of Strings
     */
    @SuppressWarnings("unchecked")
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Sets the license name of the file.
     * @param license license name as a String
     */
    public void setLicenseInformation(final String license, String licenseUrl) {
        this.license = license;

        if (!licenseUrl.startsWith("http://") && !licenseUrl.startsWith("https://")) {
            licenseUrl = "https://" + licenseUrl;
        }
        this.licenseUrl = licenseUrl;
    }

    /**
     * Sets the coordinates of where the file was created.
     * @param coordinates file coordinates as a LatLng
     */
    public void setCoordinates(@Nullable final LatLng coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Sets the categories the file falls under.
     * </p>
     * Does not append: i.e. will clear the current categories
     * and then add the specified ones.
     * @param categories file categories as a list of Strings
     */
    public void setCategories(final List<String> categories) {
        this.categories = categories;
    }

    /**
     * Get the value of requested deletion
     * @return boolean requestedDeletion
     */
    public boolean isRequestedDeletion(){
        return requestedDeletion;
    }

    /**
     * Set requested deletion to true
     * @param requestedDeletion
     */
    public void setRequestedDeletion(final boolean requestedDeletion) {
        this.requestedDeletion = requestedDeletion;
    }

    /**
     * Sets the license name of the file.
     *
     * @param license license name as a String
     */
    public void setLicense(final String license) {
        this.license = license;
    }

    /**
     * Captions are a feature part of Structured data. They are meant to store short, multilingual descriptions about files
     * This is a replacement of the previously used titles for images (titles were not multilingual)
     * Also now captions replace the previous convention of using title for filename
     *
     * This function sets captions
     * @param caption
     */
    public void setCaption(final String caption) {
        this.caption = caption;
    }

    public void setLocalUri(@Nullable final Uri localUri) {
        this.localUri = localUri;
    }

    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setDateUploaded(@Nullable final Date dateUploaded) {
        this.dateUploaded = dateUploaded;
    }

    public void setLicenseUrl(final String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    public Depictions getDepictions() {
        return depictions;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /* Sets depictions for the current media obtained fro  Wikibase API*/
    public void setDepictions(final Depictions depictions) {
        this.depictions = depictions;
    }

    /**
     * Sets the creation date of the file.
     * @param date creation date as a Date
     */
    public void setDateCreated(final Date date) {
        dateCreated = date;
    }

    /**
     * Creates a way to transfer information between two or more
     * activities.
     * @param dest Instance of Parcel
     * @param flags Parcel flag
     */
    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeParcelable(localUri, flags);
        dest.writeString(thumbUrl);
        dest.writeString(imageUrl);
        dest.writeString(filename);
        dest.writeString(thumbnailTitle);
        dest.writeString(caption);
        dest.writeString(description);
        dest.writeString(discussion);
        dest.writeLong(dataLength);
        dest.writeLong(dateCreated != null ? dateCreated.getTime() : -1);
        dest.writeLong(dateUploaded != null ? dateUploaded.getTime() : -1);
        dest.writeString(license);
        dest.writeString(licenseUrl);
        dest.writeString(creator);
        dest.writeString(pageId);
        dest.writeStringList(categories);
        dest.writeParcelable(depictions, flags);
        dest.writeByte(requestedDeletion ? (byte) 1 : (byte) 0);
        dest.writeParcelable(coordinates, flags);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Media)) {
            return false;
        }
        final Media media = (Media) o;
        return getDataLength() == media.getDataLength() &&
            isRequestedDeletion() == media.isRequestedDeletion() &&
            Objects.equals(getLocalUri(), media.getLocalUri()) &&
            Objects.equals(getThumbUrl(), media.getThumbUrl()) &&
            Objects.equals(getImageUrl(), media.getImageUrl()) &&
            Objects.equals(getFilename(), media.getFilename()) &&
            Objects.equals(getThumbnailTitle(), media.getThumbnailTitle()) &&
            Objects.equals(getCaption(), media.getCaption()) &&
            Objects.equals(getDescription(), media.getDescription()) &&
            Objects.equals(getDiscussion(), media.getDiscussion()) &&
            Objects.equals(getDateCreated(), media.getDateCreated()) &&
            Objects.equals(getDateUploaded(), media.getDateUploaded()) &&
            Objects.equals(getLicense(), media.getLicense()) &&
            Objects.equals(getLicenseUrl(), media.getLicenseUrl()) &&
            Objects.equals(getCreator(), media.getCreator()) &&
            getPageId().equals(media.getPageId()) &&
            Objects.equals(getCategories(), media.getCategories()) &&
            Objects.equals(getDepictions(), media.getDepictions()) &&
            Objects.equals(getCoordinates(), media.getCoordinates());
    }

    @Override
    public int hashCode() {
        return Objects
            .hash(getLocalUri(), getThumbUrl(), getImageUrl(), getFilename(), getThumbnailTitle(),
                getCaption(), getDescription(), getDiscussion(), getDataLength(), getDateCreated(),
                getDateUploaded(), getLicense(), getLicenseUrl(), getCreator(), getPageId(),
                getCategories(), getDepictions(), isRequestedDeletion(), getCoordinates());
    }
}
