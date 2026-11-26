package fr.free.nrw.commons.delete

/**
 * The parts of a MediaWiki query response that tell where a file is used: on other wikis
 * ("globalusage") and on Commons itself ("fileusage").
 *
 * All fields are nullable because the response is read with Gson, which leaves missing fields as
 * null. MediaWiki always lists "globalusage", even when it is empty, but leaves out
 * "fileusage" when the file is not used on Commons.
 */
data class FileUsageResponse(
    val query: Query? = null,
) {
    /**
     * The "query" object of the response
     */
    data class Query(
        val pages: List<Page>? = null,
    )

    /**
     * The file page, with the pages that use the file
     */
    data class Page(
        val title: String? = null,
        val missing: Boolean? = null,
        val globalusage: List<Usage>? = null,
        val fileusage: List<Usage>? = null,
    )

    /**
     * A page that uses the file
     */
    data class Usage(
        val title: String? = null,
    )
}
