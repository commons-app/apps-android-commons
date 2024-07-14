package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

/*"mainsnak": {
    "snaktype": "value",
    "property": "P17",
    "datavalue": {
        "value": {
            "entity-type": "item",
            "numeric-id": 30,
            "id": "Q30"
        },
        "type": "wikibase-entityid"
    },
    "datatype": "wikibase-item",
}*/
data class Snak_partial(
    @SerializedName("snaktype") val snakType: String,
    val property: String,
    @SerializedName("datavalue") val dataValue: DataValue
)
