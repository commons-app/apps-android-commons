package fr.free.nrw.commons.contributions;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import fr.free.nrw.commons.nearby.NearbyActivity;
import fr.free.nrw.commons.upload.UploadService;

public class ContributionsListFragment extends Fragment {

    public interface SourceRefresher {
        void refreshSource();
    }

    private GridView contributionsList;
    private TextView waitingMessage;
    private TextView emptyMessage;

    private ContributionController controller;
    private static final String TAG = "ContributionsList";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contributions, container, false);


        contributionsList = (GridView) v.findViewById(R.id.contributionsList);
        waitingMessage = (TextView) v.findViewById(R.id.waitingMessage);
        emptyMessage = (TextView) v.findViewById(R.id.emptyMessage);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener)getActivity());
        if(savedInstanceState != null) {
            Log.d(TAG, "Scrolling to " + savedInstanceState.getInt("grid-position"));
            contributionsList.setSelection(savedInstanceState.getInt("grid-position"));
        }

        //TODO: Should this be in onResume?
        SharedPreferences prefs = this.getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Log.d(TAG, "Last Sync Timestamp: " + lastModified);

        if (lastModified.equals("")) {
            waitingMessage.setVisibility(View.VISIBLE);
        } else {
            waitingMessage.setVisibility(View.GONE);
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
        if (outState == null) {
            outState = new Bundle();
        }
        super.onSaveInstanceState(outState);
        controller.saveState(outState);
        outState.putInt("grid-position", contributionsList.getFirstVisiblePosition());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //FIXME: must get the file data for Google Photos when receive the intent answer, in the onActivityResult method
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null) {
            Log.d("Contributions", "OnActivityResult() parameters: Result code: " + resultCode + " Data: " + data.toString());
            Uri imageData = data.getData();
            controller.handleImagePicked(requestCode, imageData);
        } else {
            Log.e("Contributions", "OnActivityResult() parameters: Result code: " + resultCode + " Data: null");
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_from_gallery:
                //Gallery crashes before reach ShareActivity screen so must implement permissions check here
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        //See http://stackoverflow.com/questions/33169455/onrequestpermissionsresult-not-being-called-in-dialog-fragment
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        return true;
                    } else {
                        controller.startGalleryPick();
                        return true;
                    }
                }
                else {
                    controller.startGalleryPick();
                    return true;
                }
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
            case R.id.menu_nearby:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        //See http://stackoverflow.com/questions/33169455/onrequestpermissionsresult-not-being-called-in-dialog-fragment
                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                        return true;
                    } else {
                        Intent nearbyIntent = new Intent(getActivity(), NearbyActivity.class);
                        startActivity(nearbyIntent);
                        return true;
                    }
                }
                else {
                    Intent nearbyIntent = new Intent(getActivity(), NearbyActivity.class);
                    startActivity(nearbyIntent);
                    return true;
                }
            case R.id.menu_refresh:
                ((SourceRefresher)getActivity()).refreshSource();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            // 1 = Storage allowed when gallery selected
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ContributionsList", "Call controller.startGalleryPick()");
                    controller.startGalleryPick();
                }
            }
            break;
            // 2 = Location allowed when 'nearby places' selected
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ContributionsList", "Location permission granted");
                    Intent nearbyIntent = new Intent(getActivity(), NearbyActivity.class);
                    startActivity(nearbyIntent);
                }
            }
            break;
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
        controller = new ContributionController(this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        controller.loadState(savedInstanceState);
    }

    protected void clearSyncMessage() {
        waitingMessage.setVisibility(View.GONE);
    }
}
