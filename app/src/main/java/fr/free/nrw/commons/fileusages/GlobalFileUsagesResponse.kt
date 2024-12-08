package fr.free.nrw.commons.fileusages

import com.google.gson.annotations.SerializedName

data class GlobalFileUsagesResponse(
    @SerializedName("continue") val continueResponse: GlobalContinue?,
    @SerializedName("batchcomplete") val batchComplete: Boolean,
    @SerializedName("query") val query: GlobalQuery,
)

data class GlobalContinue(
    @SerializedName("gucontinue") val guContinue: String,
    @SerializedName("continue") val continueKey: String
)

data class GlobalQuery(
    @SerializedName("pages") val pages: List<GlobalPage>
)

data class GlobalPage(
    @SerializedName("pageid") val pageId: Int,
    @SerializedName("ns") val nameSpace: Int,
    @SerializedName("title") val title: String,
    @SerializedName("globalusage") val fileUsage: List<GlobalFileUsage>
)

data class GlobalFileUsage(
    @SerializedName("title") val title: String,
    @SerializedName("wiki") val wiki: String,
    @SerializedName("url") val url: String
)