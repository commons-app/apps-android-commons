package org.wikipedia.wikidata

import com.google.gson.annotations.SerializedName

/*"mainsnak": {
    "snaktype": "value",
    "property": "P17",
    "datatype": "wikibase-item",
    "datavalue": {
        "value": {
        "entity-type": "item",
        "id": "Q30",
        "numeric-id": 30
    },
        "type": "wikibase-entityid"
    }
}*/
data class Snak_partial(
    @SerializedName("snaktype") val snakType: String,
    val property: String,
    @SerializedName("datavalue") val dataValue: DataValue,
    @SerializedName("datatype") val dataType: String
)
