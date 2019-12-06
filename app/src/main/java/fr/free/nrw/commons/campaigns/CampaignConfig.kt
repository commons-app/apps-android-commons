package fr.free.nrw.commons.campaigns

import com.google.gson.annotations.SerializedName

/**
 * A data class to hold the campaign configs
 */
class CampaignConfig {
    @SerializedName("showOnlyLiveCampaigns")
    private val showOnlyLiveCampaigns = false
    @SerializedName("sortBy")
    private val sortBy: String? = null
}