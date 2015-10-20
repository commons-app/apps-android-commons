package fr.free.nrw.commons.campaigns;

import android.net.Uri;
import fr.free.nrw.commons.contributions.Contribution;

import java.util.ArrayList;
import java.util.Date;

public class CampaignContribution extends Contribution {
    private Campaign campaign;

    private ArrayList<String> fieldValues;


    public CampaignContribution(Uri localUri, String remoteUri, String filename, String description, long dataLength, Date dateCreated, Date dateUploaded, String creator, String editSummary, Campaign campaign) {
        super(localUri, remoteUri, filename, description, dataLength, dateCreated, dateUploaded, creator, editSummary);
        this.campaign = campaign;
    }

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
        buffer.append("[[Category:").append(campaign.getTrackingCategory()).append("]]").append("\n");
        return buffer.toString();
    }

    @Override
    public String getDescription() {
        return super.getDescription();
    }
}
