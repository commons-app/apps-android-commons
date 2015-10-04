package org.wikimedia.commons;

import android.net.Uri;
import android.os.*;

import java.util.*;
import java.util.regex.*;

public class Media implements Parcelable {

    public static Creator<Media> CREATOR = new Creator<Media>() {
        public Media createFromParcel(Parcel parcel) {
            return new Media(parcel);
        }

        public Media[] newArray(int i) {
            return new Media[0];
        }
    };

    protected Media() {
        this.categories = new ArrayList<String>();
        this.descriptions = new HashMap<String, String>();
    }

    private HashMap<String, Object> tags = new HashMap<String, Object>();

    public Object getTag(String key) {
        return tags.get(key);
    }

    public void setTag(String key, Object value) {
        tags.put(key, value);
    }

    public static Pattern displayTitlePattern = Pattern.compile("(.*)(\\.\\w+)", Pattern.CASE_INSENSITIVE);
    public  String getDisplayTitle() {
        if(filename == null) {
            return "";
        }
        // FIXME: Gross hack bercause my regex skills suck maybe or I am too lazy who knows
        String title = filename.replaceFirst("^File:", "");
        Matcher matcher = displayTitlePattern.matcher(title);
        if(matcher.matches()) {
            return matcher.group(1);
        } else {
            return title;
        }
    }

    public String getDescriptionUrl() {
        // HACK! Geez
        return CommonsApplication.HOME_URL + "File:" + Utils.urlEncode(getFilename().replace("File:", "").replace(" ", "_"));
    }

    public Uri getLocalUri() {
        return localUri;
    }

    public String getImageUrl() {
        if(imageUrl == null) {
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

    public Date getDateUploaded() {
        return dateUploaded;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getThumbnailUrl(int width) {
        return Utils.makeThumbUrl(getImageUrl(), getFilename(), width);
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

    // Primary metadata fields
    protected Uri localUri;
    protected String imageUrl;
    protected String filename;
    protected String description; // monolingual description on input...
    protected long dataLength;
    protected Date dateCreated;
    protected Date dateUploaded;
    protected int width;
    protected int height;
    protected String license;


    protected String creator;

    protected ArrayList<String> categories; // as loaded at runtime?
    protected Map<String, String> descriptions; // multilingual descriptions as loaded

    public ArrayList<String> getCategories() {
        return (ArrayList<String>)categories.clone(); // feels dirty
    }

    public void setCategories(List<String> categories) {
        this.categories.removeAll(this.categories);
        this.categories.addAll(categories);
    }

    public void setDescriptions(Map<String,String> descriptions) {
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

    public Media(String filename) {
        this();
        this.filename = filename;
    }

    public Media(Uri localUri, String imageUrl, String filename, String description, long dataLength, Date dateCreated, Date dateUploaded, String creator) {
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

    public int describeContents() {
        return 0;
    }

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

    public Media(Parcel in) {
        localUri = (Uri)in.readParcelable(Uri.class.getClassLoader());
        imageUrl = in.readString();
        filename = in.readString();
        description = in.readString();
        dataLength = in.readLong();
        dateCreated = (Date) in.readSerializable();
        dateUploaded = (Date) in.readSerializable();
        creator = in.readString();
        tags = (HashMap<String, Object>)in.readSerializable();
        width = in.readInt();
        height = in.readInt();
        license = in.readString();
        in.readStringList(categories);
        descriptions = in.readHashMap(ClassLoader.getSystemClassLoader());
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
