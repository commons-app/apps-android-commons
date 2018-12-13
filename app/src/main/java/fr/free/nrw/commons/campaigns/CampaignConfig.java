package fr.free.nrw.commons.campaigns;

import com.google.gson.annotations.SerializedName;

/**
 * A data class to hold the campaign configs
 */
class CampaignConfig {

    @SerializedName("showOnlyLiveCampaigns") private boolean showOnlyLiveCampaigns;
    @SerializedName("sortBy") private String sortBy;
}
