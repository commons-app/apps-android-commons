package fr.free.nrw.commons;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static Pattern displayTitlePattern = Pattern.compile("(.*)(\\.\\w+)", Pattern.CASE_INSENSITIVE);
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
    private Map<String, String> descriptions; // multilingual descriptions as loaded
    private HashMap<String, Object> tags = new HashMap<>();
    private @Nullable LatLng coordinates;

    protected Media() {
        this.categories = new ArrayList<>();
        this.descriptions = new HashMap<>();
    }

    public Media(String filename) {
        this();
        this.filename = filename;
    }

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

    public Object getTag(String key) {
        return tags.get(key);
    }

    public void setTag(String key, Object value) {
        tags.put(key, value);
    }

    public String getDisplayTitle() {
        if (filename == null) {
            return "";
        }
        // FIXME: Gross hack bercause my regex skills suck maybe or I am too lazy who knows
        String title = getFilePageTitle().getDisplayText().replaceFirst("^File:", "");
        Matcher matcher = displayTitlePattern.matcher(title);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return title;
        }
    }

    public PageTitle getFilePageTitle() {
        return new PageTitle("File:" + getFilename().replaceFirst("^File:", ""));
    }

    public Uri getLocalUri() {
        return localUri;
    }

    @Nullable
    public String getImageUrl() {
        if (imageUrl == null && this.getFilename() != null) {
            imageUrl = Utils.makeThumbBaseUrl(this.getFilename());
        }
        return imageUrl;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDataLength() {
        return dataLength;
    }

    public void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date date) {
        this.dateCreated = date;
    }

    public @Nullable
    Date getDateUploaded() {
        return dateUploaded;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public @Nullable
    LatLng getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(@Nullable LatLng coordinates) {
        this.coordinates = coordinates;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<String> getCategories() {
        return (ArrayList<String>) categories.clone(); // feels dirty
    }

    public void setCategories(List<String> categories) {
        this.categories.clear();
        this.categories.addAll(categories);
    }

    void setDescriptions(Map<String, String> descriptions) {
        for (String key : this.descriptions.keySet()) {
            this.descriptions.remove(key);
        }
        for (String key : descriptions.keySet()) {
            this.descriptions.put(key, descriptions.get(key));
        }
    }

    public String getDescription(String preferredLanguage) {
        if (descriptions.containsKey(preferredLanguage)) {
            // See if the requested language is there.
            return descriptions.get(preferredLanguage);
        } else if (descriptions.containsKey("en")) {
            // Ah, English. Language of the world, until the Chinese crush us.
            return descriptions.get("en");
        } else if (descriptions.containsKey("default")) {
            // No languages marked...
            return descriptions.get("default");
        } else {
            // FIXME: return the first available non-English description?
            return "";
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

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
}
