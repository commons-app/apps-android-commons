package org.wikimedia.commons.campaigns;

import android.app.Activity;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.wikimedia.commons.R;

public class CampaignActivity extends SherlockFragmentActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campaigns);
    }


}