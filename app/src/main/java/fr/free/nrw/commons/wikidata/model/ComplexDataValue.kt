package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

data class ComplexDataValue(
        @SerializedName("type")
        var type: String? = null,
        @SerializedName("value")
        var value: ValueWithLanguageCode? = null
)