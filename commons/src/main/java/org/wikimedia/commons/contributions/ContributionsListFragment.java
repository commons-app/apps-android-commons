package org.wikimedia.commons.contributions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;


import org.wikimedia.commons.*;
import org.wikimedia.commons.R;
import org.wikimedia.commons.campaigns.Campaign;

public class ContributionsListFragment extends SherlockFragment {

    public interface CurrentCampaignProvider {
        Campaign getCurrentCampaign();
    }

    public interface SourceRefresher {
        void refreshSource();
    }

    private GridView contributionsList;
    private TextView waitingMessage;
    private TextView emptyMessage;
    private Campaign campaign;

    private ContributionController controller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contributions, container, false);
    }

    public ListAdapter getAdapter() {
        return contributionsList.getAdapter();
    }

    public void setAdapter(ListAdapter adapter) {
        this.contributionsList.setAdapter(adapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        controller.saveState(outState);
        outState.putInt("grid-position", contributionsList.getFirstVisiblePosition());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK) {
            controller.handleImagePicked(requestCode, data);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_from_gallery:
                controller.startGalleryPick();
                return true;
            case R.id.menu_from_camera:
                controller.startCameraCapture();
                return true;
            case R.id.menu_settings:
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.menu_about:
                Intent aboutIntent = new Intent(getActivity(),  AboutActivity.class);
                startActivity(aboutIntent);
                return true;
            case R.id.menu_feedback:
                Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
                feedbackIntent.setType("message/rfc822");
                feedbackIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { CommonsApplication.FEEDBACK_EMAIL });
                feedbackIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(CommonsApplication.FEEDBACK_EMAIL_SUBJECT, CommonsApplication.APPLICATION_VERSION));
                startActivity(feedbackIntent);
                return true;
            case R.id.menu_refresh:
                ((SourceRefresher)getActivity()).refreshSource();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // See http://stackoverflow.com/a/8495697/17865
        inflater.inflate(R.menu.fragment_contributions_list, menu);

        CommonsApplication app = (CommonsApplication)getActivity().getApplicationContext();
        if (!app.deviceHasCamera()) {
            menu.findItem(R.id.menu_from_camera).setEnabled(false);
        }
        if(campaign == null) {
            menu.findItem(R.id.menu_refresh).setVisible(false);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        campaign = ((CurrentCampaignProvider)getActivity()).getCurrentCampaign();
        controller = new ContributionController(this, campaign);
        controller.loadState(savedInstanceState);

        contributionsList = (GridView)getView().findViewById(R.id.contributionsList);
        waitingMessage = (TextView)getView().findViewById(R.id.waitingMessage);
        emptyMessage = (TextView)getView().findViewById(R.id.waitingMessage);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener)getActivity());
        if(savedInstanceState != null) {
            Log.d("Commons", "Scrolling to " + savedInstanceState.getInt("grid-position"));
            contributionsList.setSelection(savedInstanceState.getInt("grid-position"));
        }

        SharedPreferences prefs = this.getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        if (lastModified.equals("")) {
            waitingMessage.setVisibility(View.VISIBLE);
        }
    }

    private void clearSyncMessage() {
        waitingMessage.setVisibility(View.GONE);
    }
}
