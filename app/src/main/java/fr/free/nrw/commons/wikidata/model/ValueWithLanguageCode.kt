package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.SerializedName

data class ValueWithLanguageCode(
        @SerializedName("text")
        var text: String? = null,
        @SerializedName("language")
        var language: String? = null)