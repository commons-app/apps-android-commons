package fr.free.nrw.commons.explore.models

import android.net.Uri
import java.util.Date

/**
 * Represents a recently searched query
 * Example - query = "butterfly"
 */
class RecentSearch(
    /**
     * Modifies the content URI - marking this query as already saved in the database
     *
     * @param contentUri the content URI
     */
    var contentUri: Uri?,
    /**
     * Gets query name
     * @return query name
     */
    val query: String,
    var lastSearched: Date,
)
