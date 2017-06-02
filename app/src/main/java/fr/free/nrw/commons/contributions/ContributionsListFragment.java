package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.nearby.NearbyActivity;
import timber.log.Timber;

import static android.app.Activity.RESULT_OK;

public class ContributionsListFragment extends Fragment {

    OnTabFragmentsCreatedCallback callback;

    public interface SourceRefresher {
        void refreshSource();
    }

    @BindView(R.id.contributionsList) GridView contributionsList;
    @BindView(R.id.waitingMessage) TextView waitingMessage;
    @BindView(R.id.emptyMessage) TextView emptyMessage;

    public ContributionController controller;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contributions, container, false);
        ButterKnife.bind(this, v);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener)getActivity());
        if(savedInstanceState != null) {
            Timber.d("Scrolling to %d", savedInstanceState.getInt("grid-position"));
            contributionsList.setSelection(savedInstanceState.getInt("grid-position"));
        }

        //TODO: Should this be in onResume?
        SharedPreferences prefs = this.getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Timber.d("Last Sync Timestamp: %s", lastModified);

        if (lastModified.equals("")) {
            waitingMessage.setVisibility(View.VISIBLE);
        } else {
            waitingMessage.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        callback = (OnTabFragmentsCreatedCallback) getActivity();
        callback.onTabFragmentsCreated();
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

        if ( resultCode == RESULT_OK ) {
            Timber.d("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
            controller.handleImagePicked(requestCode, data);
        } else {
            Timber.e("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            // 1 = Storage allowed when gallery selected
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Call controller.startGalleryPick()");
                    controller.startGalleryPick();
                }
            }
            break;
            // 2 = Location allowed when 'nearby places' selected
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permission granted");
                    Intent nearbyIntent = new Intent(getActivity(), NearbyActivity.class);
                    startActivity(nearbyIntent);
                }
            }
            break;
        }
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
