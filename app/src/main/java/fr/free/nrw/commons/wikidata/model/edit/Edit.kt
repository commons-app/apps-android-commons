package fr.free.nrw.commons.wikidata.model.edit

import fr.free.nrw.commons.wikidata.mwapi.MwPostResponse

class Edit : MwPostResponse() {
    private val edit: Result? = null

    fun edit(): Result? = edit

    class Result {
        private val result: String? = null
        private val code: String? = null
        private val info: String? = null
        private val warning: String? = null

        fun editSucceeded(): Boolean =
            "Success" == result

        fun code(): String? = code

        fun info(): String? = info

        fun warning(): String? = warning
    }
}
