package org.wikimedia.commons.media;

import java.util.*;

import android.net.*;

public class Media {
    private Uri mediaUri;
    private String fileName;
    private String editSummary;
    private String mimeType;
    private String description;
    private String userName;
    
    public Media(Uri mediaUri, String fileName, String description, String editSummary, String userName) {
        this.mediaUri = mediaUri;
        this.fileName = fileName;
        this.description = description;
        this.editSummary = editSummary;
        this.userName = userName;
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
        buffer
            .append("== {{int:filedesc}} ==\n")
                .append("{{Information")
                    .append("|Description=").append(description)
                    .append("|source=").append("{{own}}")
                    .append("|author=[[User:").append(userName).append("]]")
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
