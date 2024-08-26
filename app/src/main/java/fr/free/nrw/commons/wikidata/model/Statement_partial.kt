package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

/*{
    "mainsnak": {},
    "type": "statement",
    "id": "q60$5083E43C-228B-4E3E-B82A-4CB20A22A3FB",
    "rank": "normal",
    "qualifiers": {
    "P580": [],
    "P5436": []
}
    "references": [
    {
        "hash": "d103e3541cc531fa54adcaffebde6bef28d87d32",
        "snaks": []
    }
    ]
}*/
data class Statement_partial(
    @SerializedName("mainsnak") val mainSnak: Snak_partial,
    val type: String,
    val rank: String,
    val id: String? = null,
    val qualifiers: Map<String, List<Snak_partial>> = mapOf(),
    @SerializedName("qualifiers-order") val qualifiersOrder: List<String> = listOf()
)
