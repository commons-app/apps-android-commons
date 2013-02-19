package org.wikimedia.commons;

import android.net.Uri;
import org.wikimedia.commons.contributions.Contribution;

import java.io.Serializable;
import java.util.Date;

public class Media implements Serializable {

    protected Media() {
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
}
