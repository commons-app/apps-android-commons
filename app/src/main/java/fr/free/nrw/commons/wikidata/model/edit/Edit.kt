package fr.free.nrw.commons.wikidata.model.edit

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse

class Edit : MwPostResponse() {
    @SerializedName("edit")
    private val editResult: Result? = null

    fun getEditResult(): Result? = editResult

    class Result {
        @SerializedName("result")
        private val resultStatus: String? = null
        private val code: String? = null
        private val info: String? = null
        private val warning: String? = null

        fun editSucceeded(): Boolean = "Success" == resultStatus

        fun code(): String? = code

        fun info(): String? = info

        fun warning(): String? = warning
    }
}
