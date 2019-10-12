package fr.free.nrw.commons.bookmarks;

import android.net.Uri;

public class Bookmark {
    private Uri contentUri;
    private final String mediaName;
    private final String mediaCreator;

    public Bookmark(String mediaName, String mediaCreator, Uri contentUri) {
        this.contentUri = contentUri;
        this.mediaName = mediaName == null ? "" : mediaName;
        this.mediaCreator = mediaCreator == null ? "" : mediaCreator;
    }

    /**
     * Gets the media name
     * @return the media name
     */
    public String getMediaName() {
        return mediaName;
    }


    /**
     * Gets media creator
     * @return creator name
     */
    public String getMediaCreator() { return mediaCreator;  }



    /**
     * Gets the content URI for this bookmark
     * @return content URI
     */
    public Uri getContentUri() {
        return contentUri;
    }

    /**
     * Modifies the content URI - marking this bookmark as already saved in the database
     * @param contentUri the content URI
     */
    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }
}
