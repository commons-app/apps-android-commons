package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * PageInfo model class with last revision id of the edited Wikidata entity
 */
class PageInfo(@field:Expose @field:SerializedName("lastrevid") val lastrevid: Long)