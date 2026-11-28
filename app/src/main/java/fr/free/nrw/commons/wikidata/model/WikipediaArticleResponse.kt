package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

/** Response of a Wikipedia page-properties query. */
data class WikipediaArticleResponse(
    val query: Query? = null,
) {
    /** Query result containing the requested pages. */
    data class Query(
        val pages: Map<String, Page>? = null,
    )

    /** A Wikipedia page returned by the query. */
    data class Page(
        val title: String? = null,
        val ns: Int = -1,
        val pageprops: PageProps? = null,
        val missing: String? = null,
    ) {
        /** Returns true when the page exists in the article namespace. */
        fun isExistingArticle(): Boolean = missing == null && ns == 0 && !title.isNullOrBlank()

        /** Returns the linked Wikidata item ID, if any. */
        fun wikidataId(): String? = pageprops?.wikidataId?.takeIf { it.isNotBlank() }
    }

    /** Wikibase properties attached to the page. */
    data class PageProps(
        @SerializedName("wikibase_item")
        val wikidataId: String? = null,
    )
}
