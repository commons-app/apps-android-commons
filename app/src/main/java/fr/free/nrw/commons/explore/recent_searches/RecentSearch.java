package fr.free.nrw.commons.explore.recent_searches;

import android.net.Uri;

import java.util.Date;

/**
 * Represents a category
 */
public class RecentSearch {
    private Uri contentUri;
    private String name;
    private Date lastUsed;

    public RecentSearch() {
    }

    public RecentSearch(Uri contentUri, String name, Date lastUsed) {
        this.contentUri = contentUri;
        this.name = name;
        this.lastUsed = lastUsed;
    }

    /**
     * Gets name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Modifies name
     *
     * @param name Category name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets last used date
     *
     * @return Last used date
     */
    public Date getLastUsed() {
        // warning: Date objects are mutable.
        return (Date)lastUsed.clone();
    }

    /**
     * Generates new last used date
     */
    private void touch() {
        lastUsed = new Date();
    }

    /**
     * Gets the content URI for this category
     *
     * @return content URI
     */
    public Uri getContentUri() {
        return contentUri;
    }

    /**
     * Modifies the content URI - marking this category as already saved in the database
     *
     * @param contentUri the content URI
     */
    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }

}