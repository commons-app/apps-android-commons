package fr.free.nrw.commons.wikidata.mwapi

import fr.free.nrw.commons.wikidata.model.BaseModel

class MwLegacyServiceError : BaseModel() {
    val code: String? = null
    private val info: String? = null

    val title: String
        get() = code ?: ""

    val details: String
        get() = info ?: ""
}