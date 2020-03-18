package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Tag class used when adding wikidata edit tag
 */
class EditTag(@field:Expose @field:SerializedName("revid") val revid: Int, @field:Expose @field:SerializedName("status") val status: String, @field:Expose @field:SerializedName("actionlogid") val actionlogid: Int, @field:Expose @field:SerializedName("added") val added: List<String>, @field:Expose @field:SerializedName("removed") val removed: List<Any>)