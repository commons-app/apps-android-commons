package fr.free.nrw.commons.fileusages

import com.google.gson.annotations.SerializedName


data class FileUsagesResponse(
    @SerializedName("continue") val continueResponse: CommonsContinue?,
    @SerializedName("batchcomplete") val batchComplete: Boolean,
    @SerializedName("query") val query: Query,
)

data class CommonsContinue(
    @SerializedName("fucontinue") val fuContinue: String,
    @SerializedName("continue") val continueKey: String
)

data class Query(
    @SerializedName("pages") val pages: List<Page>
)

data class Page(
    @SerializedName("pageid") val pageId: Int,
    @SerializedName("ns") val nameSpace: Int,
    @SerializedName("title") val title: String,
    @SerializedName("fileusage") val fileUsage: List<FileUsage>
)

data class FileUsage(
    @SerializedName("pageid") val pageId: Int,
    @SerializedName("ns") val nameSpace: Int,
    @SerializedName("title") val title: String,
    @SerializedName("redirect") val redirect: Boolean
)
