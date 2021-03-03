package fr.free.nrw.commons.nearby.model

import com.google.gson.annotations.SerializedName

class ResultTuple {
    @SerializedName("xml:lang")
    val lang: String
    val type: String
    val value: String

    constructor(lang: String,type: String, value: String) {
        this.lang = lang
        this.type = type
        this.value = value
    }

    constructor() {
        lang = ""
        type = ""
        value = ""
    }

}