package fr.free.nrw.commons.campaigns

import com.google.gson.annotations.SerializedName

/**
 * A data class to hold the campaign configs
 */
class CampaignConfig {
    @SerializedName("showOnlyLiveCampaigns")
    var showOnlyLiveCampaigns = false

    @SerializedName("sortBy")
    var sortBy: String? = null
}