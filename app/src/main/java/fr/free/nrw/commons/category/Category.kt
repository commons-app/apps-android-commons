package fr.free.nrw.commons.category;

import android.net.Uri;

import java.util.Date;

/**
 * Represents a category
 */
public class Category {
    private Uri contentUri;
    private String name;
    private String description;
    private String thumbnail;
    private Date lastUsed;
    private int timesUsed;

    public Category() {
    }

    public Category(Uri contentUri, String name, String description, String thumbnail, Date lastUsed, int timesUsed) {
        this.contentUri = contentUri;
        this.name = name;
        this.description = description;
        this.thumbnail = thumbnail;
        this.lastUsed = lastUsed;
        this.timesUsed = timesUsed;
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
     * Gets no. of times the category is used
     *
     * @return no. of times used
     */
    public int getTimesUsed() {
        return timesUsed;
    }

    /**
     * Increments timesUsed by 1 and sets last used date as now.
     */
    public void incTimesUsed() {
        timesUsed++;
        touch();
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

    public String getDescription() {
        return description;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setThumbnail(final String thumbnail) {
        this.thumbnail = thumbnail;
    }
}
