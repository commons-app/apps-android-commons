package fr.free.nrw.commons.nearby.model

import com.google.gson.annotations.SerializedName

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
        language = ""
        type = ""
        value = ""
    }

}