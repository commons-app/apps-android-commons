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
        return Utils.makeThumbUrl(imageUrl, filename, width);
    }

    protected Uri localUri;
    protected String imageUrl;
    protected String filename;
    protected String description;
    protected long dataLength;
    protected Date dateCreated;
    protected Date dateUploaded;


    protected String creator;


    public Media(Uri localUri, String imageUrl, String filename, String description, long dataLength, Date dateCreated, Date dateUploaded, String creator) {
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
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
