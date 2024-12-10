package fr.free.nrw.commons.wikidata.mwapi

import fr.free.nrw.commons.wikidata.model.BaseModel
import org.apache.commons.lang3.StringUtils

/**
 * Gson POJO for a MediaWiki API error.
 */
class MwServiceError : BaseModel() {
    val code: String? = null
    private val text: String? = null

    val title: String
        get() = code ?: ""

    val details: String
        get() = text ?: ""
}
