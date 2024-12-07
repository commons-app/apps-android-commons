package fr.free.nrw.commons.wikidata.mwapi

import com.google.gson.annotations.SerializedName

class MwQueryResponse : MwResponse() {
    @SerializedName("continue")
    private val continuation: Map<String, String>? = null
    private val query: MwQueryResult? = null

    fun continuation(): Map<String, String>? = continuation

    fun query(): MwQueryResult? = query

    fun success(): Boolean = query != null
}
