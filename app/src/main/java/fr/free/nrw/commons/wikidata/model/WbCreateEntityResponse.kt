package fr.free.nrw.commons.wikidata.model

import fr.free.nrw.commons.wikidata.mwapi.MwResponse

/** Response returned when a new Wikidata entity is created. */
open class WbCreateEntityResponse : MwResponse() {
    val entity: Entities.Entity? = null
}
