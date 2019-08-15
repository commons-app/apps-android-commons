package fr.free.nrw.commons.upload.structure.depictions;

import android.net.Uri;

import java.util.Date;

/**
 * Represents a Depiction
 */

public class Depiction {
    private Uri contentUri;
    private String name;
    private Date lastUsed;
    private int timesUsed;

    public Depiction() {
    }

    public Depiction(Uri contentUri, String name, Date lastUsed, int timesUsed) {
        this.contentUri = contentUri;
        this.name = name;
        this.lastUsed = lastUsed;
        this.timesUsed = timesUsed;
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
     * Modifies the content URI - marking this depiction as already saved in the database
     *
     * @param contentUri the content URI
     */

    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
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
     * @param name Depicts name
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
        return lastUsed;
    }

    /**
     * Set last used date
     *
     * @param lastUsed last used date of depiction
     */

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    /**
     * Gets no. of times the depiction is used
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
     * Generates new last used date
     */

    private void touch() {
        lastUsed = new Date();
    }
}
