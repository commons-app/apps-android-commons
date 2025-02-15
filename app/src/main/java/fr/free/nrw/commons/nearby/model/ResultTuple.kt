package fr.free.nrw.commons.nearby.model

import com.google.gson.annotations.SerializedName
import java.util.Locale

class ResultTuple {
    @SerializedName("xml:lang")
    val language: String
    val type: String
    var value: String

    constructor(lang: String, type: String, value: String) {
        this.language = lang
        this.type = type
        this.value = value
    }

    constructor() {
        language = Locale.getDefault().language
        type = ""
        value = ""
    }
}
