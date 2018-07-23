package fr.free.nrw.commons.contributions;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.nearby.NearbyActivity;
import fr.free.nrw.commons.utils.ContributionUtils;
import timber.log.Timber;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.view.View.GONE;

public class ContributionsListFragment extends CommonsDaggerSupportFragment {

    @BindView(R.id.contributionsList)
    GridView contributionsList;
    @BindView(R.id.waitingMessage)
    TextView waitingMessage;
    @BindView(R.id.loadingContributionsProgressBar)
    ProgressBar progressBar;

    @Inject
    @Named("prefs")
    SharedPreferences prefs;
    @Inject
    @Named("default_preferences")
    SharedPreferences defaultPrefs;

    private ContributionController controller;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contributions, container, false);
        ButterKnife.bind(this, v);

        contributionsList.setOnItemClickListener((AdapterView.OnItemClickListener) getActivity());
        if (savedInstanceState != null) {
            Timber.d("Scrolling to %d", savedInstanceState.getInt("grid-position"));
            contributionsList.setSelection(savedInstanceState.getInt("grid-position"));
        }

        //TODO: Should this be in onResume?
        String lastModified = prefs.getString("lastSyncTimestamp", "");
        Timber.d("Last Sync Timestamp: %s", lastModified);

        if (lastModified.equals("")) {
            waitingMessage.setVisibility(View.VISIBLE);
        } else {
            waitingMessage.setVisibility(GONE);
        }

        changeProgressBarVisibility(true);
        return v;
    }

    public ListAdapter getAdapter() {
        return contributionsList.getAdapter();
    }

    public void setAdapter(ListAdapter adapter) {
        this.contributionsList.setAdapter(adapter);

        if(BuildConfig.FLAVOR.equalsIgnoreCase("beta")){
            ((ContributionsActivity) getActivity()).betaSetUploadCount(adapter.getCount());
        }
    }

    public void changeProgressBarVisibility(boolean isVisible) {
        this.progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
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

        if (resultCode == RESULT_OK) {
            Timber.d("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
            if (requestCode == ContributionController.SELECT_FROM_CAMERA) {
                // If coming from camera, pass null as uri. Because camera photos get saved to a
                // fixed directory
                controller.handleImagePicked(requestCode, null, false, null);
            } else {
                controller.handleImagePicked(requestCode, data.getData(), false, null);
            }
        } else {
            Timber.e("OnActivityResult() parameters: Req code: %d Result code: %d Data: %s",
                    requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_from_gallery:
                //Gallery crashes before reach ShareActivity screen so must implement permissions check here
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Here, thisActivity is the current activity
                    if (ContextCompat.checkSelfPermission(getActivity(),
                            READ_EXTERNAL_STORAGE)
                            != PERMISSION_GRANTED) {

                        // Should we show an explanation?
                        if (shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE)) {

                            // Show an explanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.

                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.read_storage_permission_rationale))
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, 1);
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .create()
                                    .show();

                        } else {

                            // No explanation needed, we can request the permission.

                            requestPermissions(new String[]{READ_EXTERNAL_STORAGE},
                                    1);

                            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    } else {
                        controller.startGalleryPick();
                        return true;
                    }

                } else {
                    controller.startGalleryPick();
                    return true;
                }

                return true;
            case R.id.menu_from_camera:
                boolean useExtStorage = defaultPrefs.getBoolean("useExternalStorage", true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && useExtStorage) {
                    // Here, thisActivity is the current activity
                    if (ContextCompat.checkSelfPermission(getActivity(), WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                            // Show an explanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.
                            new AlertDialog.Builder(getActivity())
                                    .setMessage(getString(R.string.write_storage_permission_rationale))
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, 3);
                                        dialog.dismiss();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .create()
                                    .show();
                        } else {
                            // No explanation needed, we can request the permission.
                            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE},
                                    3);
                            // MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    } else {
                        controller.startCameraCapture();
                        return true;
                    }
                } else {
                    controller.startCameraCapture();
                    return true;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Timber.d("onRequestPermissionsResult: req code = " + " perm = "
                + Arrays.toString(permissions) + " grant =" + Arrays.toString(grantResults));

        switch (requestCode) {
            // 1 = Storage allowed when gallery selected
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Call controller.startGalleryPick()");
                    controller.startGalleryPick();
                }
            }
            break;
            // 2 = Location allowed when 'nearby places' selected
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                    Timber.d("Location permission granted");
                    Intent nearbyIntent = new Intent(getActivity(), NearbyActivity.class);
                    startActivity(nearbyIntent);
                }
            }
            break;
            case 3: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Call controller.startCameraCapture()");
                    controller.startCameraCapture();
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear(); // See http://stackoverflow.com/a/8495697/17865
        inflater.inflate(R.menu.fragment_contributions_list, menu);

        if (!deviceHasCamera()) {
            menu.findItem(R.id.menu_from_camera).setEnabled(false);
        }
    }

    public boolean deviceHasCamera() {
        PackageManager pm = getContext().getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
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
        waitingMessage.setVisibility(GONE);
    }

    public interface SourceRefresher {
        void refreshSource();
    }
}
