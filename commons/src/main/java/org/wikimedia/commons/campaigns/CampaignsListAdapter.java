package org.wikimedia.commons.campaigns;

import android.app.Activity;
import android.view.*;
import android.widget.*;
import org.wikimedia.commons.R;

import java.util.ArrayList;

public class CampaignsListAdapter extends BaseAdapter {
    private ArrayList<Campaign> campaigns;
    private Activity activity;

    public CampaignsListAdapter(Activity activity, ArrayList<Campaign> campaigns) {
        this.campaigns = campaigns;
        this.activity = activity;
    }

    public ArrayList<Campaign> getCampaigns() {
        return campaigns;
    }

    public void setCampaigns(ArrayList<Campaign> campaigns) {
        this.campaigns = campaigns;
    }

    public int getCount() {
        if(campaigns == null) {
            return 0;
        }
        return campaigns.size();
    }

    public Object getItem(int i) {
        return campaigns.get(i);
    }

    public long getItemId(int i) {
        return i;
    }

    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null) {
            view = activity.getLayoutInflater().inflate(R.layout.layout_campaign_item, null);
        }

        TextView campaignName = (TextView)view.findViewById(R.id.campaignItemName);

        Campaign campaign = campaigns.get(i);

        campaignName.setText(campaign.getName());
        return view;
    }
}
