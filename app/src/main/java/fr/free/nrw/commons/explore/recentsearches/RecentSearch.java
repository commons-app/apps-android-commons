package fr.free.nrw.commons.explore.recentsearches;

import android.net.Uri;
import java.util.Date;

/**
 * Represents a recently searched query
 * query - butterfly
 */
public class RecentSearch {
    private Uri contentUri;
    private String query;
    private Date lastUsed;

    public RecentSearch(Uri contentUri, String query, Date lastUsed) {
        this.contentUri = contentUri;
        this.query = query;
        this.lastUsed = lastUsed;
    }

    /**
     * Gets name
     *
     * @return name
     */
    public String getQuery() {
        return query;
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
     * Gets the content URI for this query
     *
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