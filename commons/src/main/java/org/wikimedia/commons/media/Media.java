package org.wikimedia.commons.media;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import android.net.*;

public class Media {
    private Uri mediaUri;
    private String fileName;
    private String editSummary;
    private String mimeType;
    private String description;
    private String userName;
    private Date dateCreated;
    private Date dateUploaded;
    
    public Media(Uri mediaUri, String fileName, String description, String editSummary, String userName, Date dateCreated) {
        this.mediaUri = mediaUri;
        this.fileName = fileName;
        this.description = description;
        this.editSummary = editSummary;
        this.userName = userName;
        this.dateCreated = dateCreated;
    }
    
    public Uri getMediaUri() {
        return mediaUri;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getEditSummary() {
        return editSummary;
    }
    
    public String getPageContents() {
        StringBuffer buffer = new StringBuffer();
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
        buffer
            .append("== {{int:filedesc}} ==\n")
                .append("{{Information")
                    .append("|Description=").append(description)
                    .append("|source=").append("{{own}}")
                    .append("|author=[[User:").append(userName).append("]]");
        if(dateCreated != null) {
            buffer
                    .append("|date={{According to EXIF data|").append(isoFormat.format(dateCreated)).append("}}");
        }
        buffer
                .append("}}").append("\n")
            .append("== {{int:license-header}} ==\n")
                .append("{{self|cc-by-sa-3.0}}")
            ;
        return buffer.toString();
    }
    
    public String getMimeType() {
        return mimeType;
    }
}
