package fr.free.nrw.commons.wikidata.model

/**
 * Model class for Depiction item returned from API after calling searchForDepicts
 */
class DepictSearchItem(
    val id: String,
    val pageid: String,
    val url: String,
    val label: String?,
    val description: String?
)
