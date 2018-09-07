package fr.free.nrw.commons.contributions;

import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.HandlerService;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.NearbyPlaces;
import fr.free.nrw.commons.notification.Notification;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.upload.UploadService;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.Contribution.STATE_FAILED;
import static fr.free.nrw.commons.contributions.ContributionDao.Table.ALL_FIELDS;
import static fr.free.nrw.commons.contributions.ContributionsContentProvider.BASE_URI;
import static fr.free.nrw.commons.settings.Prefs.UPLOADS_SHOWING;

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

    private ArrayList<DataSetObserver> observersWaitingForLoad = new ArrayList<>();
    private Cursor allContributions;
    private UploadService uploadService;
    private boolean isUploadServiceConnected;

    public TextView numberOfUploads;
    public ProgressBar numberOfUploadsProgressBar;

    private ContributionsListFragment contributionsListFragment;
    private MediaDetailPagerFragment mediaDetailPagerFragment;
    public static final String CONTRIBUTION_LIST_FRAGMENT_TAG = "ContributionListFragmentTag";
    public static final String MEDIA_DETAIL_PAGER_FRAGMENT_TAG = "MediaDetailFragmentTag";

    /**
     * Since we will need to use parent activity on onAuthCookieAcquired, we have to wait
     * fragment to be attached. Latch will be responsible for this sync.
     */
    private ServiceConnection uploadServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            uploadService = (UploadService) ((HandlerService.HandlerServiceLocalBinder) binder)
                    .getService();
            isUploadServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            // this should never happen
            Timber.e(new RuntimeException("UploadService died but the rest of the process did not!"));
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributions, container, false);
        numberOfUploads = view.findViewById(R.id.numOfUploads);

        numberOfUploadsProgressBar = view.findViewById(R.id.progressBar);
        numberOfUploadsProgressBar.setVisibility(View.VISIBLE);
        numberOfUploadsProgressBar.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(getActivity(), R.color.white), PorterDuff.Mode.SRC_IN );

        if (savedInstanceState != null) {
            mediaDetailPagerFragment = (MediaDetailPagerFragment)getChildFragmentManager().findFragmentByTag(MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
            contributionsListFragment = (ContributionsListFragment) getChildFragmentManager().findFragmentByTag(CONTRIBUTION_LIST_FRAGMENT_TAG);
            //getSupportLoaderManager().initLoader(0, null, this);
            if (savedInstanceState.getBoolean("mediaDetailsVisible")) {
                setMediaDetailPagerFragment();
            } else {
                setContributionsListFragment();
            }
        } else {
            setContributionsListFragment();
        }

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
        // show tabs on contribution list is visible
        ((ContributionsActivity)getActivity()).showTabs();

        // Create if null
        if (getContributionsListFragment() == null) {
            contributionsListFragment =  new ContributionsListFragment();
        }
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        // When this container fragment is created, we fill it with our ContributionsListFragment
        transaction.replace(R.id.root_frame, contributionsListFragment, CONTRIBUTION_LIST_FRAGMENT_TAG);
        transaction.addToBackStack(CONTRIBUTION_LIST_FRAGMENT_TAG);
        transaction.commit();
        getChildFragmentManager().executePendingTransactions();
    }

    /**
     * Replace FrameLayout with MediaDetailPagerFragment, user will see details of selected media.
     * Creates new one if null.
     */
    public void setMediaDetailPagerFragment() {
        // hide tabs on media detail view is visible
        ((ContributionsActivity)getActivity()).hideTabs();

        // Create if null
        if (getMediaDetailPagerFragment() == null) {
            mediaDetailPagerFragment =  new MediaDetailPagerFragment();
        }
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        // When this container fragment is created, we fill it with our MediaDetailPagerFragment
        //transaction.addToBackStack(null);
        transaction.add(R.id.root_frame, mediaDetailPagerFragment, MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
        transaction.addToBackStack(MEDIA_DETAIL_PAGER_FRAGMENT_TAG);
        transaction.commit();
        getChildFragmentManager().executePendingTransactions();
    }

    /**
     * Just getter method of ContributionsListFragment child of ContributionsFragment
     * @return contributionsListFragment, if any created
     */
    public ContributionsListFragment getContributionsListFragment() {
        return contributionsListFragment;
    }

    /**
     * Just getter method of MediaDetailPagerFragment child of ContributionsFragment
     * @return mediaDetailsFragment, if any created
     */
    public MediaDetailPagerFragment getMediaDetailPagerFragment() {
        return mediaDetailPagerFragment;
    }

    @Override
    public void onBackStackChanged() {
        ((ContributionsActivity)getActivity()).initBackButton();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        int uploads = prefs.getInt(UPLOADS_SHOWING, 100);
        return new CursorLoader(getActivity(), BASE_URI, //TODO find out the reason we pass activity here
                ALL_FIELDS, "", null,
                ContributionDao.CONTRIBUTION_SORT + "LIMIT " + uploads);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (contributionsListFragment != null) {
            contributionsListFragment.changeProgressBarVisibility(false);

            if (contributionsListFragment.getAdapter() == null) {
                Log.d("deneme", "contributionsListFragment adapter set");
                contributionsListFragment.setAdapter(new ContributionsListAdapter(getActivity().getApplicationContext(),
                        cursor, 0, contributionDao));
            } else {
                ((CursorAdapter) contributionsListFragment.getAdapter()).swapCursor(cursor);
            }

            contributionsListFragment.clearSyncMessage();
            notifyAndMigrateDataSetObservers();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        ((CursorAdapter) contributionsListFragment.getAdapter()).swapCursor(null);
    }

    private void notifyAndMigrateDataSetObservers() {
        Adapter adapter = contributionsListFragment.getAdapter();

        // First, move the observers over to the adapter now that we have it.
        for (DataSetObserver observer : observersWaitingForLoad) {
            adapter.registerDataSetObserver(observer);
        }
        observersWaitingForLoad.clear();

        // Now fire off a first notification...
        for (DataSetObserver observer : observersWaitingForLoad) {
            observer.onChanged();
        }
    }

    /**
     * Called when onAuthCookieAcquired is called on authenticated parent activity
     * @param uploadServiceIntent
     */
    public void onAuthCookieAcquired(Intent uploadServiceIntent) {
        // Since we call onAuthCookieAcquired method from onAttach, isAdded is still false. So don't use it

        if (getActivity() != null) { // If fragment is attached to parent activity
            getActivity().bindService(uploadServiceIntent, uploadServiceConnection, Context.BIND_AUTO_CREATE);
            allContributions = contributionDao.loadAllContributions();
            getActivity().getSupportLoaderManager().initLoader(0, null, ContributionsFragment.this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // show detail at a position
        showDetail(i);
    }

    /**
     * Replace whatever is in the current contributionsFragmentContainer view with
     * mediaDetailPagerFragment, and preserve previous state in back stack.
     * Called when user selects a contribution.
     */
    private void showDetail(int i) {
        if (mediaDetailPagerFragment == null || !mediaDetailPagerFragment.isVisible()) {
            mediaDetailPagerFragment = new MediaDetailPagerFragment();
            setMediaDetailPagerFragment();
        }
        mediaDetailPagerFragment.showImage(i);
    }

    /**
     * Retry upload when it is failed
     * @param i position of upload which will be retried
     */
    public void retryUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = contributionDao.fromCursor(allContributions);
        if (c.getState() == STATE_FAILED) {
            uploadService.queue(UploadService.ACTION_UPLOAD_FILE, c);
            Timber.d("Restarting for %s", c.toString());
        } else {
            Timber.d("Skipping re-upload for non-failed %s", c.toString());
        }
    }

    /**
     * Delete a failed upload attempt
     * @param i position of upload attempt which will be deteled
     */
    public void deleteUpload(int i) {
        allContributions.moveToPosition(i);
        Contribution c = contributionDao.fromCursor(allContributions);
        if (c.getState() == STATE_FAILED) {
            Timber.d("Deleting failed contrib %s", c.toString());
            contributionDao.delete(c);
        } else {
            Timber.d("Skipping deletion for non-failed contrib %s", c.toString());
        }
    }

    @Override
    public void refreshSource() {
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Media getMediaAtPosition(int i) {
        if (contributionsListFragment.getAdapter() == null) {
            // not yet ready to return data
            return null;
        } else {
            return contributionDao.fromCursor((Cursor) contributionsListFragment.getAdapter().getItem(i));
        }
    }

    @Override
    public int getTotalMediaCount() {
        if (contributionsListFragment.getAdapter() == null) {
            return 0;
        }
        return contributionsListFragment.getAdapter().getCount();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsListFragment.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.add(observer);
        } else {
            adapter.registerDataSetObserver(observer);
        }
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        Adapter adapter = contributionsListFragment.getAdapter();
        if (adapter == null) {
            observersWaitingForLoad.remove(observer);
        } else {
            adapter.unregisterDataSetObserver(observer);
        }
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

