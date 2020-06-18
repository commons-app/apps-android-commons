package fr.free.nrw.commons;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import fr.free.nrw.commons.location.LatLng;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.wikipedia.page.PageTitle;

@Entity
public class Media implements Parcelable {

    private String thumbUrl;
    private String imageUrl;
    private String filename;
    private String fallbackDescription; // monolingual description on input...
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
    @Nullable private  LatLng coordinates;
    @NotNull
    private Map<String, String> captions = Collections.emptyMap();
    @NotNull
    private Map<String, String> descriptions = Collections.emptyMap();

    @NotNull
    private List<String> depictionIds = Collections.emptyList();

    /**
     * Provides local constructor
     */
    public Media() {
        pageId = UUID.randomUUID().toString();
    }

    /**
     * Constructor with all parameters
     */
    public Media(final String thumbUrl,
        final String imageUrl,
        final String filename,
        final String fallbackDescription,
        @Nullable final Date dateUploaded,
        final String license,
        final String licenseUrl,
        final String creator,
        @NonNull final String pageId,
        final List<String> categories,
        @Nullable final LatLng coordinates,
        @NotNull final Map<String, String> captions,
        @NotNull final Map<String, String> descriptions,
        @NotNull final List<String> depictionIds) {
        this.thumbUrl = thumbUrl;
        this.imageUrl = imageUrl;
        this.filename = filename;
        this.fallbackDescription = fallbackDescription;
        this.dateUploaded = dateUploaded;
        this.license = license;
        this.licenseUrl = licenseUrl;
        this.creator = creator;
        this.pageId = pageId;
        this.categories = categories;
        this.coordinates = coordinates;
        this.captions = captions;
        this.descriptions = descriptions;
        this.depictionIds = depictionIds;
    }

    public Media(Media media) {
        this(media.getThumbUrl(), media.getImageUrl(), media.getFilename(),
            media.getFallbackDescription(), media.getDateUploaded(), media.getLicense(),
            media.getLicenseUrl(), media.getCreator(), media.getPageId(), media.getCategories(),
            media.getCoordinates(), media.getCaptions(), media.getDescriptions(),
            media.getDepictionIds());
    }

    public Media(final String filename,
        Map<String, String> captions, final String fallbackDescription,
        final String creator, final List<String> categories) {
        this();
        thumbUrl = null;
        this.imageUrl = null;
        this.filename = filename;
        this.fallbackDescription = fallbackDescription;
        this.dateUploaded = new Date();
        this.creator = creator;
        this.categories = categories;
        this.captions=captions;
    }

    protected Media(final Parcel in) {
        this(in.readString(), in.readString(), in.readString(),
            in.readString(), readDateUploaded(in), in.readString(),
            in.readString(), in.readString(), in.readString(), readList(in),
            in.readParcelable(LatLng.class.getClassLoader()),
            ((Map<String, String>) in.readSerializable()),
            ((Map<String, String>) in.readSerializable()),
            readList(in));
    }

    private static List<String> readList(Parcel in) {
        final List<String> list = new ArrayList<>();
        in.readStringList(list);
        return list;
    }

    private static Date readDateUploaded(Parcel in) {
        final long tmpDateUploaded = in.readLong();
        return tmpDateUploaded == -1 ? null : new Date(tmpDateUploaded);
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

    @Nullable
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
     * Gets file page title
     * @return New media page title
     */
    @NonNull public PageTitle getPageTitle() {
        return Utils.getPageTitle(getFilename());
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
     * Gets the file description.
     * @return file description as a string
     */
    public String getFallbackDescription() {
        return fallbackDescription;
    }

    /**
     * Sets the name of the file.
     * @param filename the new name of the file
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * Sets the file description.
     * @param fallbackDescription the new description of the file
     */
    public void setFallbackDescription(final String fallbackDescription) {
        this.fallbackDescription = fallbackDescription;
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
     * Gets the license name of the file.
     * @return license as a String
     */
    public String getLicense() {
        return license;
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
    public List<String> getCategories() {
        return categories;
    }

    /**
     * Sets the coordinates of where the file was created.
     * @param coordinates file coordinates as a LatLng
     */
    public void setCoordinates(@Nullable final LatLng coordinates) {
        this.coordinates = coordinates;
    }

    /**
     * Returns wikicode to use the media file on a MediaWiki site
     * @return
     */
    public String getWikiCode() {
        return String.format("[[%s|thumb|%s]]", filename, getMostRelevantCaption());
    }

    private String getMostRelevantCaption() {
        final String languageAppropriateCaption = captions.get(Locale.getDefault().getLanguage());
        if(languageAppropriateCaption!=null){
            return languageAppropriateCaption;
        }
        for (String firstCaption : captions.values()) {
            return firstCaption;
        }
        return getDisplayTitle();
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
     * Sets the license name of the file.
     *
     * @param license license name as a String
     */
    public void setLicense(final String license) {
        this.license = license;
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
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(thumbUrl);
        dest.writeString(imageUrl);
        dest.writeString(filename);
        dest.writeString(fallbackDescription);
        dest.writeLong(dateUploaded != null ? dateUploaded.getTime() : -1);
        dest.writeString(license);
        dest.writeString(licenseUrl);
        dest.writeString(creator);
        dest.writeString(pageId);
        dest.writeStringList(categories);
        dest.writeParcelable(coordinates, flags);
        dest.writeSerializable((Serializable) captions);
        dest.writeSerializable((Serializable) descriptions);
        dest.writeList(depictionIds);
    }

    public Map<String, String> getCaptions() {
        return captions;
    }

    public void setCaptions(Map<String, String> captions) {
        this.captions = captions;
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Map<String, String> descriptions) {
        this.descriptions = descriptions;
    }

    public List<String> getDepictionIds() {
        return depictionIds;
    }

    public void setDepictionIds(final List<String> depictionIds) {
        this.depictionIds = depictionIds;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Media media = (Media) o;
        return Objects.equals(thumbUrl, media.thumbUrl) &&
            Objects.equals(imageUrl, media.imageUrl) &&
            Objects.equals(filename, media.filename) &&
            Objects.equals(fallbackDescription, media.fallbackDescription) &&
            Objects.equals(dateUploaded, media.dateUploaded) &&
            Objects.equals(license, media.license) &&
            Objects.equals(licenseUrl, media.licenseUrl) &&
            Objects.equals(creator, media.creator) &&
            pageId.equals(media.pageId) &&
            Objects.equals(categories, media.categories) &&
            Objects.equals(coordinates, media.coordinates) &&
            captions.equals(media.captions) &&
            descriptions.equals(media.descriptions) &&
            depictionIds.equals(media.depictionIds);
    }

    @Override
    public int hashCode() {
        return Objects
            .hash(thumbUrl, imageUrl, filename, fallbackDescription, dateUploaded, license,
                licenseUrl,
                creator, pageId, categories, coordinates, captions, descriptions, depictionIds);
    }
}
