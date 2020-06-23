package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.WikidataProperties
import org.wikipedia.wikidata.Snak_partial

data class WikidataSetClaim(
    @SerializedName("mainsnak") val mainSnak: Snak_partial,
    val id: String,
    val qualifiers: Map<String, List<Snak_partial>> = mapOf(),
    val type: String = "statement",
    val rank: String = "normal",
    @SerializedName("qualifiers-order") val qualifiersOrder: List<String>
) {
    constructor(
        mainSnak: Snak_partial,
        id: String,
        p2096: List<Snak_partial>
    ) : this(
        mainSnak,
        id,
        mapOf(WikidataProperties.MEDIA_LEGENDS.propertyName to p2096),
        "statement",
        "normal",
        listOf(WikidataProperties.MEDIA_LEGENDS.propertyName)
    )
}



