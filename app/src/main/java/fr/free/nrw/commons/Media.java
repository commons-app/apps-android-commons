package fr.free.nrw.commons;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.media.Depictions;
import fr.free.nrw.commons.utils.CommonsDateUtil;
import fr.free.nrw.commons.utils.MediaDataExtractorUtil;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private String user;
    /**
     * Wikibase Identifier associated with media files
     */
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
    }

    /**
     * Provides a minimal constructor
     *
     * @param filename Media filename
     */
    public Media(String filename) {
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
     * @param user Media username
     */
    public Media(Uri localUri, String imageUrl, String filename,
        String description,
        long dataLength, Date dateCreated, Date dateUploaded, String creator, String user) {
        this.localUri = localUri;
        this.thumbUrl = imageUrl;
        this.imageUrl = imageUrl;
        this.filename = filename;
        this.description = description;
        this.dataLength = dataLength;
        this.dateCreated = dateCreated;
        this.dateUploaded = dateUploaded;
        this.creator = creator;
        this.user = user;
    }

    public Media(Uri localUri, String filename,
        String description, String creator, String user, List<String> categories) {
        this(localUri,null, filename,
            description, -1, null, new Date(), creator, user);
        this.categories = categories;
    }

    public Media(String title, Date date, String creator, String user) {
        this(null, null, title, "", -1, date, date, creator, user);
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
    public static Media from(MwQueryPage page) {
        ImageInfo imageInfo = page.imageInfo();
        if (imageInfo == null) {
            return new Media(); // null is not allowed
        }
        ExtMetadata metadata = imageInfo.getMetadata();
        if (metadata == null) {
            Media media = new Media(null, imageInfo.getOriginalUrl(),
                    page.title(), "", 0, null, null, null, imageInfo.getUser());
            if (!StringUtils.isBlank(imageInfo.getThumbUrl())) {
                media.setThumbUrl(imageInfo.getThumbUrl());
            }
            return media;
        }

        Media media = new Media(null,
                imageInfo.getOriginalUrl(),
                page.title(),
            "",
                0,
                safeParseDate(metadata.dateTime()),
                safeParseDate(metadata.dateTime()),
                getArtist(metadata),
                imageInfo.getUser()
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
        String latitude = metadata.getGpsLatitude();
        String longitude = metadata.getGpsLongitude();

        if (!StringUtils.isBlank(latitude) && !StringUtils.isBlank(longitude)) {
            LatLng latLng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude), 0);
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
    private static String getArtist(ExtMetadata metadata) {
        try {
            String artistHtml = metadata.artist();
            final String anchorStartTagTerminalChars = "\">";
            final String anchorCloseTag = "</a>";

            return artistHtml.substring(
                artistHtml.indexOf(anchorStartTagTerminalChars) + anchorStartTagTerminalChars
                    .length(), artistHtml.indexOf(anchorCloseTag));
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * @return pageId for the current media object*/
    public String getPageId() {
        return pageId;
    }

    /**
     *sets pageId for the current media object
     */
    public void setPageId(String pageId) {
        this.pageId = pageId;
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

    /**
     * Set Caption(if available) as the thumbnail title of the image
     */
    public void setThumbnailTitle(String title) {
        this.thumbnailTitle = title;
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
     * Sets the name of the file.
     * @param filename the new name of the file
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Sets the discussion of the file.
     * @param discussion
     */
    public void setDiscussion(String discussion) {
        this.discussion = discussion;
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
     * Sets the file description.
     * @param description the new description of the file
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the dataLength of the file.
     * @return file dataLength as a long
     */
    public long getDataLength() {
        return dataLength;
    }

    /**
     * Sets the dataLength of the file.
     * @param dataLength as a long
     */
    public void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }

    /**
     * Gets the creation date of the file.
     * @return creation date as a Date
     */
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
     * Sets the creation date of the file.
     * @param date creation date as a Date
     */
    public void setDateCreated(Date date) {
        this.dateCreated = date;
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
     * Gets the name of the username.
     * @return username as a String
     */
    public String getUser() {
        return user;
    }

    public void setUser(String user) { this.user = user; }

    /**
     * Gets the name of the creator of the file.
     * @return creator name as a String
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Sets the creator name of the file.
     * @param creator creator name as a string
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }

    /**
     * Gets the license name of the file.
     * @return license as a String
     */
    public String getLicense() {
        return license;
    }

    public void setThumbUrl(String thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    /**
     * Sets the license name of the file.
     * @param license license name as a String
     */
    public void setLicenseInformation(String license, String licenseUrl) {
        this.license = license;

        if (!licenseUrl.startsWith("http://") && !licenseUrl.startsWith("https://")) {
            licenseUrl = "https://" + licenseUrl;
        }
        this.licenseUrl = licenseUrl;
    }

    /**
     * Gets the coordinates of where the file was created.
     * @return file coordinates as a LatLng
     */
    public @Nullable
    LatLng getCoordinates() {
        return coordinates;
    }

    /**
     * Sets the coordinates of where the file was created.
     * @param coordinates file coordinates as a LatLng
     */
    public void setCoordinates(@Nullable LatLng coordinates) {
        this.coordinates = coordinates;
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
     * Sets the categories the file falls under.
     * </p>
     * Does not append: i.e. will clear the current categories
     * and then add the specified ones.
     * @param categories file categories as a list of Strings
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    @Nullable private static Date safeParseDate(String dateStr) {
        try {
            return CommonsDateUtil.getIso8601DateFormatShort().parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Set requested deletion to true
     * @param requestedDeletion
     */
    public void setRequestedDeletion(boolean requestedDeletion){
        this.requestedDeletion = requestedDeletion;
    }

    /**
     * Get the value of requested deletion
     * @return boolean requestedDeletion
     */
    public boolean isRequestedDeletion(){
        return requestedDeletion;
    }

    /**
     * Sets the license name of the file.
     *
     * @param license license name as a String
     */
    public void setLicense(String license) {
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
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /* Sets depictions for the current media obtained fro  Wikibase API*/
    public void setDepictions(Depictions depictions) {
        this.depictions = depictions;
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

    /**
     * Creates a way to transfer information between two or more
     * activities.
     * @param dest Instance of Parcel
     * @param flags Parcel flag
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.localUri, flags);
        dest.writeString(this.thumbUrl);
        dest.writeString(this.imageUrl);
        dest.writeString(this.filename);
        dest.writeString(this.thumbnailTitle);
        dest.writeString(this.caption);
        dest.writeString(this.description);
        dest.writeString(this.discussion);
        dest.writeLong(this.dataLength);
        dest.writeLong(this.dateCreated != null ? this.dateCreated.getTime() : -1);
        dest.writeLong(this.dateUploaded != null ? this.dateUploaded.getTime() : -1);
        dest.writeString(this.license);
        dest.writeString(this.licenseUrl);
        dest.writeString(this.creator);
        dest.writeString(this.user);
        dest.writeString(this.pageId);
        dest.writeStringList(this.categories);
        dest.writeParcelable(this.depictions, flags);
        dest.writeByte(this.requestedDeletion ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.coordinates, flags);
    }

    protected Media(Parcel in) {
        this.localUri = in.readParcelable(Uri.class.getClassLoader());
        this.thumbUrl = in.readString();
        this.imageUrl = in.readString();
        this.filename = in.readString();
        this.thumbnailTitle = in.readString();
        this.caption = in.readString();
        this.description = in.readString();
        this.discussion = in.readString();
        this.dataLength = in.readLong();
        long tmpDateCreated = in.readLong();
        this.dateCreated = tmpDateCreated == -1 ? null : new Date(tmpDateCreated);
        long tmpDateUploaded = in.readLong();
        this.dateUploaded = tmpDateUploaded == -1 ? null : new Date(tmpDateUploaded);
        this.license = in.readString();
        this.licenseUrl = in.readString();
        this.creator = in.readString();
        this.user = in.readString();
        this.pageId = in.readString();
        final ArrayList<String> list = new ArrayList<>();
        in.readStringList(list);
        this.categories=list;
        in.readParcelable(Depictions.class.getClassLoader());
        this.requestedDeletion = in.readByte() != 0;
        this.coordinates = in.readParcelable(LatLng.class.getClassLoader());
    }

    public static final Creator<Media> CREATOR = new Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel source) {
            return new Media(source);
        }

        @Override
        public Media[] newArray(int size) {
            return new Media[size];
        }
    };
}
