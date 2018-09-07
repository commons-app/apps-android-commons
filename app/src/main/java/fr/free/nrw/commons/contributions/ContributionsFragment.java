package fr.free.nrw.commons.contributions;

import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationController;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ContributionsFragment
        extends CommonsDaggerSupportFragment
        implements  LoaderManager.LoaderCallbacks<Cursor>,
                    AdapterView.OnItemClickListener,
                    MediaDetailPagerFragment.MediaDetailProvider,
                    FragmentManager.OnBackStackChangedListener,
                    ContributionsListFragment.SourceRefresher {
    @Inject
    @Named("default_preferences")
    SharedPreferences prefs;
    @Inject
    ContributionDao contributionDao;
    @Inject
    MediaWikiApi mediaWikiApi;
    @Inject
    NotificationController notificationController;

    public TextView numberOfUploads;
    public ProgressBar numberOfUploadsProgressBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions, container, false);
        numberOfUploads = view.findViewById(R.id.numOfUploads);

        numberOfUploadsProgressBar = view.findViewById(R.id.progressBar);
        numberOfUploadsProgressBar.setVisibility(View.VISIBLE);
        numberOfUploadsProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(getActivity(), R.color.white), PorterDuff.Mode.SRC_IN );
        

        if(!BuildConfig.FLAVOR.equalsIgnoreCase("beta")){
            setUploadCount();
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    /**
     * Replace FrameLayout with ContributionsListFragment, user will see contributions list.
     * Creates new one if null.
     */
    public void setContributionsListFragment() {

    }

    /**
     * Replace FrameLayout with MediaDetailPagerFragment, user will see details of selected media.
     * Creates new one if null.
     */
    public void setMediaDetailPagerFragment() {

    }

    /**
     * Just getter method of ContributionsListFragment child of ContributionsFragment
     * @return contributionsListFragment, if any created
     */
    public NewContributionsListFragment getContributionsListFragment() {
        return null;
    }

    /**
     * Just getter method of MediaDetailPagerFragment child of ContributionsFragment
     * @return mediaDetailsFragment, if any created
     */
    public MediaDetailPagerFragment getMediaDetailPagerFragment() {
        return null;
    }




    @Override
    public void onBackStackChanged() {

    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /**
     * Called when onAuthCookieAcquired is called on authenticated parent activity
     * @param uploadServiceIntent
     */
    public void onAuthCookieAcquired(Intent uploadServiceIntent) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack.
     * Called when user selects a contribution.
     */
    private void showDetail(int i) {

    }

    /**
     * Retry upload when it is failed
     * @param i position of upload which will be retried
     */
    public void retryUpload(int i) {

    }

    /**
     * Delete a failed upload attempt
     * @param i position of upload attempt which will be deteled
     */
    public void deleteUpload(int i) {

    }

    @Override
    public void refreshSource() {

    }

    @Override
    public Media getMediaAtPosition(int i) {
        return null;
    }

    @Override
    public int getTotalMediaCount() {
        return 0;
    }

    @Override
    public void notifyDatasetChanged() {

    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }


    @SuppressWarnings("ConstantConditions")
    private void setUploadCount() {

    }


    private void displayUploadCount(Integer uploadCount) {

    }

    public void betaSetUploadCount(int betaUploadCount) {
        displayUploadCount(betaUploadCount);
    }

    /**
     * Updates notification indicator on toolbar to indicate there are unread notifications
     * @param unreadNotifications
     */
    public void updateNotificationsNotification(List<Notification> unreadNotifications) {

    }

    /**
     * Update nearby indicator on cardview on main screen
     * @param nearbyPlaces
     */
    public void updateNearbyNotification(List<NearbyPlaces> nearbyPlaces) {

    }
}

