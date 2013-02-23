package org.wikimedia.commons;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import org.wikimedia.commons.contributions.Contribution;

import java.io.Serializable;
import java.util.Date;

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

    public String getDescription() {
        return description;
    }

    public long getDataLength() {
        return dataLength;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public Date getDateUploaded() {
        return dateUploaded;
    }

    public String getCreator() {
        return creator;
    }

    public String getThumbnailUrl(int width) {
        return Utils.makeThumbUrl(imageUrl, filename, width);
    }

    public String getDisplayTitle() {
        return Utils.displayTitleFromTitle(filename);
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
    }
}
