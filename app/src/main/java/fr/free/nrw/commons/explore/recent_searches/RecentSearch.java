package fr.free.nrw.commons.explore.recent_searches;

import android.net.Uri;
import java.util.Date;

/**
 * Represents a recently searched query
 */
public class RecentSearch {
    private Uri contentUri;
    private String name;
    private Date lastUsed;

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