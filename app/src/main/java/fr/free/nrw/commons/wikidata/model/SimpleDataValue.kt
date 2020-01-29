package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

data class SimpleDataValue(
        @SerializedName("type")
        var type: String? = null,
        @SerializedName("value")
        var value: String? = null)