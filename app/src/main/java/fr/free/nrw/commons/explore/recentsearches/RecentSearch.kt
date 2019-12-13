package fr.free.nrw.commons.explore.recentsearches

import android.net.Uri
import java.util.*

/**
 * Represents a recently searched query
 * Example - query = "butterfly"
 */
class RecentSearch
/**
 * Constructor
 * @param contentUri the content URI for this query
 * @param query query name
 * @param lastSearched last searched date
 */(
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
        val query: String, var lastSearched: Date) {

}