package fr.free.nrw.commons.media

import com.google.gson.annotations.SerializedName

/**
 * Model class for parsing Captions when fetching captions using filename in MediaClient
 */
class Caption() {
    @SerializedName("language")
    var language: String? = null

    @SerializedName("value")
    var value: String? = null

    constructor(language: String?, value: String?) : this() {
        this.language = language
        this.value = value
    }
}
