package fr.free.nrw.commons.contributions;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static fr.free.nrw.commons.di.NetworkingModule.NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE;

import android.Manifest.permission;
import android.content.Context;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver;
import androidx.recyclerview.widget.RecyclerView.ItemAnimator;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.SimpleItemAnimator;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;


/**
 * Created by root on 01.06.2018.
 */

public class ContributionsListFragment extends CommonsDaggerSupportFragment implements
    ContributionsListContract.View, ContributionsListAdapter.Callback,
    WikipediaInstructionsDialogFragment.Callback {

    private static final String RV_STATE = "rv_scroll_state";

    @BindView(R.id.contributionsList)
    RecyclerView rvContributionsList;
    @BindView(R.id.loadingContributionsProgressBar)
    ProgressBar progressBar;
    @BindView(R.id.fab_plus)
    FloatingActionButton fabPlus;
    @BindView(R.id.fab_camera)
    FloatingActionButton fabCamera;
    @BindView(R.id.fab_gallery)
    FloatingActionButton fabGallery;
    @BindView(R.id.noContributionsYet)
    TextView noContributionsYet;
    @BindView(R.id.fab_layout)
    LinearLayout fab_layout;
    @BindView(R.id.fab_custom_gallery)
    FloatingActionButton fabCustomGallery;

    @Inject
    SystemThemeUtils systemThemeUtils;
    @BindView(R.id.tv_contributions_of_user)
    AppCompatTextView tvContributionsOfUser;

    @Inject
    ContributionController controller;
    @Inject
    MediaClient mediaClient;

    @Named(NAMED_LANGUAGE_WIKI_PEDIA_WIKI_SITE)
    @Inject
    WikiSite languageWikipediaSite;

    @Inject
    ContributionsListPresenter contributionsListPresenter;

    @Inject
    SessionManager sessionManager;

    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;
    private Animation rotate_backward;


    private boolean isFabOpen;

    private ContributionsListAdapter adapter;

    @Nullable
    private Callback callback;

    private final int SPAN_COUNT_LANDSCAPE = 3;
    private final int SPAN_COUNT_PORTRAIT = 1;

    private int contributionsSize;
    String userName;
    private ActivityResultLauncher<String[]> inAppCameraLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean areAllGranted = true;
            for (final boolean b : result.values()) {
                areAllGranted = areAllGranted && b;
            }

            if (areAllGranted) {
                controller.locationPermissionCallback.onLocationPermissionGranted();
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    controller.handleShowRationaleFlowCameraLocation(getActivity());
                } else {
                    controller.locationPermissionCallback.onLocationPermissionDenied(
                        getActivity().getString(R.string.in_app_camera_location_permission_denied));
                }
            }
        }
    });


    @Override
    public void onCreate(
        @Nullable @org.jetbrains.annotations.Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Now that we are allowing this fragment to be started for
        // any userName- we expect it to be passed as an argument
        if (getArguments() != null) {
            userName = getArguments().getString(ProfileActivity.KEY_USERNAME);
        }

        if (StringUtils.isEmpty(userName)) {
            userName = sessionManager.getUserName();
        }
    }

    @Override
    public View onCreateView(
        final LayoutInflater inflater, @Nullable final ViewGroup container,
        @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_contributions_list, container, false);
        ButterKnife.bind(this, view);
        contributionsListPresenter.onAttachView(this);

        if (Objects.equals(sessionManager.getUserName(), userName)) {
            tvContributionsOfUser.setVisibility(GONE);
            fab_layout.setVisibility(VISIBLE);
        } else {
            tvContributionsOfUser.setVisibility(VISIBLE);
            tvContributionsOfUser.setText(getString(R.string.contributions_of_user, userName));
            fab_layout.setVisibility(GONE);
        }

        initAdapter();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() != null && getParentFragment() instanceof ContributionsFragment) {
            callback = ((ContributionsFragment) getParentFragment());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;//To avoid possible memory leak
    }

    private void initAdapter() {
        adapter = new ContributionsListAdapter(this, mediaClient);
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initRecyclerView();
        initializeAnimations();
        setListeners();
    }

    private void initRecyclerView() {
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(),
            getSpanCount(getResources().getConfiguration().orientation));
        rvContributionsList.setLayoutManager(layoutManager);

        //Setting flicker animation of recycler view to false.
        final ItemAnimator animator = rvContributionsList.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        contributionsListPresenter.setup(userName,
            Objects.equals(sessionManager.getUserName(), userName));
        contributionsListPresenter.contributionList.observe(getViewLifecycleOwner(), list -> {
            contributionsSize = list.size();
            adapter.submitList(list);
            if (callback != null) {
                callback.notifyDataSetChanged();
            }
        });
        rvContributionsList.setAdapter(adapter);
        adapter.registerAdapterDataObserver(new AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                contributionsSize = adapter.getItemCount();
                if (callback != null) {
                    callback.notifyDataSetChanged();
                }
                if (itemCount > 0 && positionStart == 0) {
                    if (adapter.getContributionForPosition(positionStart) != null) {
                        rvContributionsList
                            .scrollToPosition(0);//Newly upload items are always added to the top
                    }
                }
            }

            /**
             * Called whenever items in the list have changed
             * Calls viewPagerNotifyDataSetChanged() that will notify the viewpager
             */
            @Override
            public void onItemRangeChanged(final int positionStart, final int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
                if (callback != null) {
                    callback.viewPagerNotifyDataSetChanged();
                }
            }
        });

        //Fab close on touch outside (Scrolling or taping on item triggers this action).
        rvContributionsList.addOnItemTouchListener(new OnItemTouchListener() {

            /**
             * Silently observe and/or take over touch events sent to the RecyclerView before
             * they are handled by either the RecyclerView itself or its child views.
             */
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    if (isFabOpen) {
                        animateFAB(isFabOpen);
                    }
                }
                return false;
            }

            /**
             * Process a touch event as part of a gesture that was claimed by returning true
             * from a previous call to {@link #onInterceptTouchEvent}.
             *
             * @param rv
             * @param e  MotionEvent describing the touch event. All coordinates are in the
             *           RecyclerView's coordinate system.
             */
            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                //required abstract method DO NOT DELETE
            }

            /**
             * Called when a child of RecyclerView does not want RecyclerView and its ancestors
             * to intercept touch events with {@link ViewGroup#onInterceptTouchEvent(MotionEvent)}.
             *
             * @param disallowIntercept True if the child does not want the parent to intercept
             *                          touch events.
             */
            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                //required abstract method DO NOT DELETE
            }

        });
    }

    private int getSpanCount(final int orientation) {
        return orientation == Configuration.ORIENTATION_LANDSCAPE ?
            SPAN_COUNT_LANDSCAPE : SPAN_COUNT_PORTRAIT;
    }

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // check orientation
        fab_layout.setOrientation(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ?
            LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        rvContributionsList
            .setLayoutManager(
                new GridLayoutManager(getContext(), getSpanCount(newConfig.orientation)));
    }

    private void initializeAnimations() {
        fab_open = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getActivity(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getActivity(), R.anim.rotate_backward);
    }

    private void setListeners() {
        fabPlus.setOnClickListener(view -> animateFAB(isFabOpen));
        fabCamera.setOnClickListener(view -> {
            controller.initiateCameraPick(getActivity(), inAppCameraLocationPermissionLauncher);
            animateFAB(isFabOpen);
        });
        fabGallery.setOnClickListener(view -> {
            controller.initiateGalleryPick(getActivity(), true);
            animateFAB(isFabOpen);
        });
    }

    /**
     * Launch Custom Selector.
     */
    @OnClick(R.id.fab_custom_gallery)
    void launchCustomSelector() {
        controller.initiateCustomGalleryPickWithPermission(getActivity());
        animateFAB(isFabOpen);
    }

    public void scrollToTop() {
        rvContributionsList.smoothScrollToPosition(0);
    }

    private void animateFAB(final boolean isFabOpen) {
        this.isFabOpen = !isFabOpen;
        if (fabPlus.isShown()) {
            if (isFabOpen) {
                fabPlus.startAnimation(rotate_backward);
                fabCamera.startAnimation(fab_close);
                fabGallery.startAnimation(fab_close);
                fabCustomGallery.startAnimation(fab_close);
                fabCamera.hide();
                fabGallery.hide();
                fabCustomGallery.hide();
            } else {
                fabPlus.startAnimation(rotate_forward);
                fabCamera.startAnimation(fab_open);
                fabGallery.startAnimation(fab_open);
                fabCustomGallery.startAnimation(fab_open);
                fabCamera.show();
                fabGallery.show();
                fabCustomGallery.show();
            }
            this.isFabOpen = !isFabOpen;
        }
    }

    /**
     * Shows welcome message if user has no contributions yet i.e. new user.
     */
    @Override
    public void showWelcomeTip(final boolean shouldShow) {
        noContributionsYet.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    /**
     * Responsible to set progress bar invisible and visible
     *
     * @param shouldShow True when contributions list should be hidden.
     */
    @Override
    public void showProgress(final boolean shouldShow) {
        progressBar.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    @Override
    public void showNoContributionsUI(final boolean shouldShow) {
        noContributionsYet.setVisibility(shouldShow ? VISIBLE : GONE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        final GridLayoutManager layoutManager = (GridLayoutManager) rvContributionsList
            .getLayoutManager();
        outState.putParcelable(RV_STATE, layoutManager.onSaveInstanceState());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (null != savedInstanceState) {
            final Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(RV_STATE);
            rvContributionsList.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }

    @Override
    public void retryUpload(final Contribution contribution) {
        if (null != callback) {//Just being safe, ideally they won't be called when detached
            callback.retryUpload(contribution);
        }
    }

    @Override
    public void deleteUpload(final Contribution contribution) {
        DialogUtil.showAlertDialog(getActivity(),
            String.format(Locale.getDefault(),
                getString(R.string.cancelling_upload)),
            String.format(Locale.getDefault(),
                getString(R.string.cancel_upload_dialog)),
            String.format(Locale.getDefault(), getString(R.string.yes)), String.format(Locale.getDefault(), getString(R.string.no)),
            () -> {
                ViewUtil.showShortToast(getContext(), R.string.cancelling_upload);
                contributionsListPresenter.deleteUpload(contribution);
                CommonsApplication.cancelledUploads.add(contribution.getPageId());
            }, () -> {
                // Do nothing
            });
    }

    @Override
    public void openMediaDetail(final int position, boolean isWikipediaButtonDisplayed) {
        if (null != callback) {//Just being safe, ideally they won't be called when detached
            callback.showDetail(position, isWikipediaButtonDisplayed);
        }
    }

    /**
     * Handle callback for wikipedia icon clicked
     *
     * @param contribution
     */
    @Override
    public void addImageToWikipedia(Contribution contribution) {
        DialogUtil.showAlertDialog(getActivity(),
            getString(R.string.add_picture_to_wikipedia_article_title),
            getString(R.string.add_picture_to_wikipedia_article_desc),
            () -> {
                showAddImageToWikipediaInstructions(contribution);
            }, () -> {
                // do nothing
            });
    }

    /**
     * Pauses the current upload
     *
     * @param contribution
     */
    @Override
    public void pauseUpload(Contribution contribution) {
        ViewUtil.showShortToast(getContext(), R.string.pausing_upload);
        callback.pauseUpload(contribution);
    }

    /**
     * Resumes the current upload
     *
     * @param contribution
     */
    @Override
    public void resumeUpload(Contribution contribution) {
        ViewUtil.showShortToast(getContext(), R.string.resuming_upload);
        callback.retryUpload(contribution);
    }

    /**
     * Display confirmation dialog with instructions when the user tries to add image to wikipedia
     *
     * @param contribution
     */
    private void showAddImageToWikipediaInstructions(Contribution contribution) {
        FragmentManager fragmentManager = getFragmentManager();
        WikipediaInstructionsDialogFragment fragment = WikipediaInstructionsDialogFragment
            .newInstance(contribution);
        fragment.setCallback(this::onConfirmClicked);
        fragment.show(fragmentManager, "WikimediaFragment");
    }


    public Media getMediaAtPosition(final int i) {
        if (adapter.getContributionForPosition(i) != null) {
            return adapter.getContributionForPosition(i).getMedia();
        }
        return null;
    }

    public int getTotalMediaCount() {
        return contributionsSize;
    }

    /**
     * Open the editor for the language Wikipedia
     *
     * @param contribution
     */
    @Override
    public void onConfirmClicked(@Nullable Contribution contribution, boolean copyWikicode) {
        if (copyWikicode) {
            String wikicode = contribution.getMedia().getWikiCode();
            Utils.copy("wikicode", wikicode, getContext());
        }

        final String url =
            languageWikipediaSite.mobileUrl() + "/wiki/" + contribution.getWikidataPlace()
                .getWikipediaPageTitle();
        Utils.handleWebUrl(getContext(), Uri.parse(url));
    }

    public Integer getContributionStateAt(int position) {
        return adapter.getContributionForPosition(position).getState();
    }

    public interface Callback {

        void notifyDataSetChanged();

        void retryUpload(Contribution contribution);

        void showDetail(int position, boolean isWikipediaButtonDisplayed);

        void pauseUpload(Contribution contribution);

        // Notify the viewpager that number of items have changed.
        void viewPagerNotifyDataSetChanged();
    }
}
