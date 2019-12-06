package fr.free.nrw.commons.campaigns;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data class to hold the response from the campaigns api
 */
public class CampaignResponseDTO {

    @SerializedName("config")
    private CampaignConfig campaignConfig;

    @SerializedName("campaigns")
    private List<Campaign> campaigns;

    public CampaignConfig getCampaignConfig() {
        return campaignConfig;
    }

    public List<Campaign> getCampaigns() {
        return campaigns;
    }
}
