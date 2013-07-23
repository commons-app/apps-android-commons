package org.wikimedia.commons.campaigns;

import org.wikimedia.commons.contributions.Contribution;

import java.util.ArrayList;

public class CampaignContribution extends Contribution {
    private Campaign campaign;

    private ArrayList<String> fieldValues;

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    @Override
    public String getTrackingTemplates() {
        StringBuffer buffer = new StringBuffer();
        if(campaign.getAutoAddWikitext() != null) {
            buffer.append(campaign.getAutoAddWikitext()).append("\n");
        }
        if(campaign.getAutoAddCategories() != null && campaign.getAutoAddCategories().size() != 0) {
            for(String cat : campaign.getAutoAddCategories()) {
                buffer.append("[[Category:").append(cat).append("]]").append("\n");
            }
        } else {
            buffer.append("{{subst:unc}}\n");
        }
        return buffer.toString();
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }
}
