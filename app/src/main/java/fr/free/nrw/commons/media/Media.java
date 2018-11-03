package fr.free.nrw.commons.media;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.free.nrw.commons.PageTitle;
import fr.free.nrw.commons.utils.Utils;
import fr.free.nrw.commons.location.LatLng;

public class Media implements Parcelable {

    public static Creator<Media> CREATOR = new Creator<Media>() {
        @Override
        public Media createFromParcel(Parcel parcel) {
            return new Media(parcel);
        }

        @Override
        public Media[] newArray(int i) {
            return new Media[0];
        }
    };

    // Primary metadata fields
    protected Uri localUri;
    protected String imageUrl;
    protected String filename;
    protected String description; // monolingual description on input...
    protected long dataLength;
    protected Date dateCreated;
    protected @Nullable Date dateUploaded;
    protected int width;
    protected int height;
    protected String license;
    protected String creator;
    protected ArrayList<String> categories; // as loaded at runtime?
    protected boolean requestedDeletion;
    private Map<String, String> descriptions; // multilingual descriptions as loaded
    private HashMap<String, Object> tags = new HashMap<>();
    private @Nullable LatLng coordinates;

    /**
     * Provides local constructor
     */
    protected Media() {
        this.categories = new ArrayList<>();
        this.descriptions = new HashMap<>();
    }

    /**
     * Provides a minimal constructor
     *
     * @param filename Media filename
     */
    public Media(String filename) {
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
    public Media(Uri localUri, String imageUrl, String filename, String description,
                 long dataLength, Date dateCreated, @Nullable Date dateUploaded, String creator) {
        this();
        this.localUri = localUri;
        this.imageUrl = imageUrl;
        this.filename = filename;
        this.description = description;
        this.dataLength = dataLength;
        this.dateCreated = dateCreated;
        this.dateUploaded = dateUploaded;
        this.creator = creator;
    }

    @SuppressWarnings("unchecked")
    public Media(Parcel in) {
        localUri = in.readParcelable(Uri.class.getClassLoader());
        imageUrl = in.readString();
        filename = in.readString();
        description = in.readString();
        dataLength = in.readLong();
        dateCreated = (Date) in.readSerializable();
        dateUploaded = (Date) in.readSerializable();
        creator = in.readString();
        tags = (HashMap<String, Object>) in.readSerializable();
        width = in.readInt();
        height = in.readInt();
        license = in.readString();
        if (categories != null) {
            in.readStringList(categories);
        }
        descriptions = in.readHashMap(ClassLoader.getSystemClassLoader());
    }

    /**
     * Gets tag of media
     * @param key Media key
     * @return Media tag
     */
    public Object getTag(String key) {
        return tags.get(key);
    }

    /**
     * Modifies( or creates a) tag of media
     * @param key Media key
     * @param value Media value
     */
    public void setTag(String key, Object value) {
        tags.put(key, value);
    }

    /**
     * Gets media display title
     * @return Media display title. If this.filename is null, returns "Untitled"
     */
    public String getDisplayTitle() {
        if (getFilename() == null) {
            return "Untitled";
        }

        // Get filename without "File:" prefix and with spaces instead of underscores
        String title = getPageTitle().getDisplayKey();

        // Remove extension if present
        if (title.contains(".")) {
            title = title.substring(0, title.lastIndexOf('.'));
        }

        return title;
    }

    /**
     * Gets media's page title as a PageTitle object
     * @return Media's page title as a PageTitle object
     * @throws NullPointerException If filename is null
     */
    public PageTitle getPageTitle() {
        if (getFilename() == null) {
            throw new NullPointerException("Filename is null");
        }

        if (getFilename().startsWith("File:")) {
            return new PageTitle(getFilename());
        }
        return new PageTitle("File:" + getFilename());
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
     * @return Image URL
     */
    @Nullable
    public String getImageUrl() {
        if (imageUrl == null && this.getFilename() != null) {
            imageUrl = Utils.makeThumbBaseUrl(this.getFilename());
        }
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
     * Gets the file description.
     * @return file description as a string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the file description.
     * @param description the new description of the file
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the datalength of the file.
     * @return file datalength as a long
     */
    public long getDataLength() {
        return dataLength;
    }

    /**
     * Sets the datalength of the file.
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
     * Gets the width of the media.
     * @return file width as an int
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the media.
     * @param width file width as an int
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the height of the media.
     * @return file height as an int
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the media.
     * @param height file height as an int
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the license name of the file.
     * @return license as a String
     */
    public String getLicense() {
        return license;
    }

    /**
     * Sets the license name of the file.
     * @param license license name as a String
     */
    public void setLicense(String license) {
        this.license = license;
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
    public ArrayList<String> getCategories() {
        return (ArrayList<String>) categories.clone(); // feels dirty
    }

    /**
     * Sets the categories the file falls under.
     * </p>
     * Does not append: i.e. will clear the current categories
     * and then add the specified ones.
     * @param categories file categories as a list of Strings
     */
    public void setCategories(List<String> categories) {
        this.categories.clear();
        this.categories.addAll(categories);
    }

    /**
     * Modifies (or sets) media descriptions
     * @param descriptions Media descriptions
     */
    public void setDescriptions(Map<String, String> descriptions) {
        this.descriptions.clear();
        this.descriptions.putAll(descriptions);
    }

    /**
     * Gets media description in the preferred language if possible.
     * If there is no description in the preferred language it falls back to English.
     * If there is no description in English it'll return in any language it can.
     * If there are no descriptions, it returns an empty string.
     * @param preferredLanguage Preferred language
     * @return Description as a string. Might not be in preferred language, also might be empty.
     */
    public String getDescription(String preferredLanguage) {
        // If there are no descriptions, return an empty string
        if (descriptions.isEmpty()) {
            return "";
        }

        // Try to get the description in preferredLanguage
        if (descriptions.containsKey(preferredLanguage)) {
            return descriptions.get(preferredLanguage);
        }

        // Ah, English. Language of the world, until the Chinese crush us.
        if (descriptions.containsKey("en")) {
            return descriptions.get("en");
        }

        // Fall back to default description
        if (descriptions.containsKey("default")) {
            return descriptions.get("default");
        }

        // Getting desperate now... Fallback to any description
        return descriptions.values().toArray(new String[0])[0];
    }

    /**
     * Method of Parcelable interface
     * @return zero
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Creates a way to transfer information between two or more
     * activities.
     * @param parcel Instance of Parcel
     * @param flags Parcel flag
     */
    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeParcelable(localUri, flags);
        parcel.writeString(imageUrl);
        parcel.writeString(filename);
        parcel.writeString(description);
        parcel.writeLong(dataLength);
        parcel.writeSerializable(dateCreated);
        parcel.writeSerializable(dateUploaded);
        parcel.writeString(creator);
        parcel.writeSerializable(tags);
        parcel.writeInt(width);
        parcel.writeInt(height);
        parcel.writeString(license);
        parcel.writeStringList(categories);
        parcel.writeMap(descriptions);
    }

    public void setRequestedDeletion(){
        requestedDeletion = true;
    }

    public boolean getRequestedDeletion(){
        return requestedDeletion;
    }
}
