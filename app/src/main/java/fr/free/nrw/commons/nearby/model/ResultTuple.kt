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
        // Setting the default language to "en" to ensure users
        // are aware that they can select a language while uploading.
        // This prevents cases where no language is selected.
        language = "en"
        type = ""
        value = ""
    }
}
