package fr.free.nrw.commons.campaigns

import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.campaigns.models.Campaign

/**
 * Data class to hold the response from the campaigns api
 */
class CampaignResponseDTO {
    @SerializedName("config")
    val campaignConfig: CampaignConfig? = null
    @SerializedName("campaigns")
    val campaigns: List<Campaign>? = null

}