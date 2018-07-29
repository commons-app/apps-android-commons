package fr.free.nrw.commons.explore.recentsearches;

import android.net.Uri;
import java.util.Date;

/**
 * Represents a recently searched query
 * Example - query = "butterfly"
 */
public class RecentSearch {
    private Uri contentUri;
    private String query;
    private Date lastSearched;

    /**
     * Constructor
     * @param contentUri the content URI for this query
     * @param query query name
     * @param lastSearched last searched date
     */
    public RecentSearch(Uri contentUri, String query, Date lastSearched) {
        this.contentUri = contentUri;
        this.query = query;
        this.lastSearched = lastSearched;
    }

    /**
     * Gets query name
     * @return query name
     */
    public String getQuery() {
        return query;
    }

    /**
     * Gets last searched date
     * @return Last searched date
     */
    public Date getLastSearched() {
        // warning: Date objects are mutable.
        return (Date)lastSearched.clone();
    }


    /**
     * Updates the last searched date
     * @param  lastSearched Last searched date
     */
    public void setLastSearched(Date lastSearched) {
        this.lastSearched = lastSearched;
    }

    /**
     * Gets the content URI for this query
     * @return content URI
     */
    public Uri getContentUri() {
        return contentUri;
    }

    /**
     * Modifies the content URI - marking this query as already saved in the database
     *
     * @param contentUri the content URI
     */
    public void setContentUri(Uri contentUri) {
        this.contentUri = contentUri;
    }

}