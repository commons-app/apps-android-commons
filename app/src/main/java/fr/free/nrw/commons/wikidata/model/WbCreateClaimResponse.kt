package fr.free.nrw.commons.wikidata.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * Wikidata create claim response model class
 */
class WbCreateClaimResponse(@field:Expose @field:SerializedName("pageinfo") val pageinfo: PageInfo, @field:Expose @field:SerializedName("success") val success: Int)