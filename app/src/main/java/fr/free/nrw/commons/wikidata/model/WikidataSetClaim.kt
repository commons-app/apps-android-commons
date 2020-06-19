package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName
import org.wikipedia.wikidata.*

data class WikidataSetClaim(
    @SerializedName("mainsnak") val mainSnak: Snak_partial,
    val id: String,
    val references: List<Reference>,
    val type: String = "statement",
    val rank: String = "normal"
) {
    constructor(mainSnak: Snak_partial,
                id: String,
                references: List<Reference>): this(mainSnak, id, references, "statement", "normal")
}

data class Reference(
    val snaks: Map<String, Snak_partial> = mapOf(),
    @SerializedName("snaks-order") val snaksOrder: List<String>
) {
    companion object {
        @JvmStatic
        fun from(propertyName: String, snakPartial: Snak_partial) =
            listOf(
                Reference(
                    mapOf(Pair(propertyName, snakPartial)),
                    listOf(propertyName)
                )
            )
    }
}



