package fr.free.nrw.commons.bookmarks;

import android.net.Uri;

public class Bookmark {
    public Uri contentUri;
    private String mediaName;
    private String mediaCreator;

    public Bookmark(String mediaName, String mediaCreator) {
        this.contentUri = BookmarkContentProvider.uriForName(mediaName);
        this.mediaName = mediaName == null ? "" : mediaName;
        this.mediaCreator = mediaCreator == null ? "" : mediaCreator;
    }

    /**
     * Gets the file name
     *
     * @return the file name
     */
    public String getMediaName() {
        return mediaName;
    }

    /**
     * Modifies name
     *
     * @param name Category name
     */
    public void setMediaName(String name) {
        this.mediaName = name;
    }

    /**
     * Gets media creator
     *
     * @return creator name
     */
    public String getMediaCreator() { return mediaCreator;  }


    public boolean isEqual(String name, String creator, String creationDate) {
        return (name.equals(this.mediaName) && creator.equals(this.mediaCreator));
    }


    /**
     * Gets the content URI for this bookmark
     *
     * @return content URI
     */
    public Uri getContentUri() {
        return contentUri;
    }

    /**
     * Modifies the content URI - marking this bookmark as already saved in the database
     *
     * @param contentUri the content URI
     */
    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }
}
