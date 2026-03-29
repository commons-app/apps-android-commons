package fr.free.nrw.commons.wikidata.mwapi

import fr.free.nrw.commons.wikidata.model.BaseModel

class MwErrorResponse : BaseModel() {
    val error: MwLegacyServiceError? = null
}