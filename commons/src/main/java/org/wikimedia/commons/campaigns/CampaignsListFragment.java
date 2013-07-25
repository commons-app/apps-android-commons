package org.wikimedia.commons.campaigns;

import android.os.*;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import org.wikimedia.commons.*;

import java.util.*;

public class CampaignsListFragment extends SherlockFragment {
    private ArrayList<Campaign> campaigns;
    private CampaignsListAdapter listAdapter;
    private GridView campaignsListView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_campaigns, container, false);
        campaignsListView = (GridView)view.findViewById(R.id.campaignsList);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listAdapter = new CampaignsListAdapter(getActivity(), campaigns);
        campaignsListView.setAdapter(listAdapter);
        Utils.executeAsyncTask(new FetchCampaignsTask(getActivity()){
            @Override
            protected void onPostExecute(ArrayList<Campaign> campaigns) {
                super.onPostExecute(campaigns);
                CampaignsListFragment.this.campaigns = campaigns;
                listAdapter.setCampaigns(campaigns);
                listAdapter.notifyDataSetChanged();
            }
        });
    }
}