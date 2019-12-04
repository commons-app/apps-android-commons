package fr.free.nrw.commons.campaigns

import com.google.gson.annotations.SerializedName

/**
 * Data class to hold the response from the campaigns api
 */
class CampaignResponseDTO {
    @SerializedName("config")
    val campaignConfig: CampaignConfig? = null
    @SerializedName("campaigns")
    val campaigns: List<Campaign>? = null

}