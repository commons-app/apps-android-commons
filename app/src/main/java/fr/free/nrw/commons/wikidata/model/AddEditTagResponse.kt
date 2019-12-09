package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Response class for add edit tag
 */
class AddEditTagResponse {
    @SerializedName("tag")
    @Expose
    var tag: List<EditTag>? = null
}