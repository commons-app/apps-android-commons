package fr.free.nrw.commons.bookmarks;

import android.net.Uri;

public class Bookmark {
    public Uri contentUri;
    private String mediaName;
    private String mediaCreator;
    private String mediaCreationDate;

    public Bookmark(Uri contentUri, String mediaName, String mediaCreator, String mediaCreationDate) {
        this.contentUri = contentUri;
        this.mediaName = mediaName;
        this.mediaCreator = mediaCreator;
        this.mediaCreationDate = mediaCreationDate;
    }

    /**
     * Gets mediaName
     *
     * @return mediaName
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
     * Gets last used date
     *
     * @return Last used date
     */
    public String getMediaCreator() { return mediaCreator;  }

    /**
     * Gets no. of times the category is used
     *
     * @return no. of times used
     */
    public String getMediaCreationDate() {
        return mediaCreationDate;
    }


    public boolean isEqual(String name, String creator, String creationDate) {
        return (name.equals(this.mediaName) && creator.equals(this.mediaCreator) && creationDate.equals(this.mediaCreationDate));
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
