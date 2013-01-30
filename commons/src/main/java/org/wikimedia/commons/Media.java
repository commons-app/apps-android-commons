package org.wikimedia.commons;

import android.net.Uri;

import java.io.Serializable;
import java.util.Date;

public class Media implements Serializable {

    public Uri getLocalUri() {
        return localUri;
    }

    public Uri getRemoteUri() {
        return remoteUri;
    }

    public String getFilename() {
        return filename;
    }

    public String getDescription() {
        return description;
    }

    public String getCommonsURL() {
        return commonsURL;
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


    protected Uri localUri;
    protected Uri remoteUri;
    protected String filename;
    protected String description;
    protected String commonsURL;
    protected long dataLength;
    protected Date dateCreated;
    protected Date dateUploaded;


    protected String creator;


    public Media(Uri localUri, Uri remoteUri, String filename, String description, String commonsURL, long dataLength, Date dateCreated, Date dateUploaded, String creator) {
        this.localUri = localUri;
        this.remoteUri = remoteUri;
        this.filename = filename;
        this.description = description;
        this.commonsURL = commonsURL;
        this.dataLength = dataLength;
        this.dateCreated = dateCreated;
        this.dateUploaded = dateUploaded;
        this.creator = creator;
    }
}
