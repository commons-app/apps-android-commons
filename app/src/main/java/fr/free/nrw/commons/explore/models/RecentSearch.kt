package fr.free.nrw.commons.explore.models

import android.net.Uri
import java.util.Date

class RecentSearch(
    /**
     * The content URI that marks this query as already saved in the database.
     * @property contentUri the content URI
     */
//    @Deprecated("Required for legacy ContentProvider DAO compatibility only")
//    var contentUri: Uri? = null,
    /**
     * Gets query name
     * @return query name
     */
    val query: String,
    var lastSearched: Date,
)
