package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

/*"value": {
    "entity-type": "item",
    "id": "Q30",
    "numeric-id": 30
}*/
data class WikiBaseEntityValue(
    @SerializedName("entity-type") val entityType: String,
    val id: String,
    val numericId: Long
)
