package fr.free.nrw.commons.contributions;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.SettingsActivity;

public class ContributionsListFragment extends Fragment {

    public interface SourceRefresher {
        void refreshSource();
    }

    private GridView contributionsList;
    private TextView waitingMessage;
    private TextView emptyMessage;

    private fr.free.nrw.commons.contributions.ContributionController controller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contributions, container, false);

        contributionsList = (GridView) v.findViewById(R.id.contributionsList);
        waitingMessage = (TextView) v.findViewById(R.id.waitingMessage);
        emptyMessage = (TextView) v.findViewById(R.id.waitingMessage);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener)getActivity());
        if(savedInstanceState != null) {
            Log.d("Commons", "Scrolling to " + savedInstanceState.getInt("grid-position"));
            contributionsList.setSelection(savedInstanceState.getInt("grid-position"));
        }

        SharedPreferences prefs = this.getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Log.d("Commons", "Last Sync Timestamp: " + lastModified);

        if (lastModified.equals("")) {
            waitingMessage.setVisibility(View.VISIBLE);
        } else {
        }

        return v;
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

                try {
                    startActivity(feedbackIntent);
                }
                catch (ActivityNotFoundException e) {
                    Toast.makeText(getActivity(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
                }

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

        menu.findItem(R.id.menu_refresh).setVisible(false);

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

        controller = new fr.free.nrw.commons.contributions.ContributionController(this);
        controller.loadState(savedInstanceState);
    }

    private void clearSyncMessage() {
        waitingMessage.setVisibility(View.GONE);
    }
}
