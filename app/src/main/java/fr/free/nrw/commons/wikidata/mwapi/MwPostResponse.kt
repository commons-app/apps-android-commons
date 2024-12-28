package fr.free.nrw.commons.wikidata.mwapi

import com.google.gson.annotations.SerializedName

open class MwPostResponse : MwResponse() {
    @SerializedName("success")
    val successVal: Int = 0

    fun success(result: String?): Boolean =
        "success" == result
}

