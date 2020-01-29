package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

data class Reference(
        @SerializedName("snaks")
        var snaks: Map<String, ComplexSnak>? = null,
        @SerializedName("snaks-order")
        var snaksOrder: List<String>? = null)