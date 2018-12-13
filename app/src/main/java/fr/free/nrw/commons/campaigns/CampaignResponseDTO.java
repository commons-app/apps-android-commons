package fr.free.nrw.commons.campaigns;

import com.google.gson.annotations.SerializedName;
import java.util.List;

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
