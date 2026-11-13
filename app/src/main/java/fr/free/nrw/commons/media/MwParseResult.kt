package fr.free.nrw.commons.media

import com.google.gson.annotations.SerializedName

@Suppress("UnusedPrivateProperty") // Fields deserialized by Gson via reflection
class MwParseResult {
    private val pageid = 0
    private val index = 0
    private val text: MwParseText? = null

    fun text(): String? = text?.text

    inner class MwParseText {
        @SerializedName("*")
        internal val text: String? = null
    }
}
