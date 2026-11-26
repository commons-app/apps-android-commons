package fr.free.nrw.commons.category

/**
 * The parts of a MediaWiki query response needed to resolve category redirects: title
 * normalization, hard redirects, membership of "Category:Category redirects" and the category
 * links of a page.
 *
 * All fields are nullable because the response is read with Gson, which leaves missing fields as
 * null.
 */
data class CategoryRedirectResponse(
    val query: Query? = null,
) {
    /**
     * The "query" object of the response
     */
    data class Query(
        val normalized: List<TitleMapping>? = null,
        val redirects: List<TitleMapping>? = null,
        val pages: List<Page>? = null,
    )

    /**
     * A title that MediaWiki changed, either while normalizing it or by following a hard redirect
     */
    data class TitleMapping(
        val from: String? = null,
        val to: String? = null,
    )

    /**
     * A page of the response, with the categories and links that were asked for
     */
    data class Page(
        val title: String? = null,
        val categories: List<Title>? = null,
        val links: List<Title>? = null,
    )

    /**
     * A category or a linked page, given by its title
     */
    data class Title(
        val title: String? = null,
    )
}
