package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADED;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADING;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.PAGE_SIZE;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.START_OFFSET;

import android.accounts.Account;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.MergeAdapter;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.databinding.FragmentLeaderboardBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.utils.ConfigUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.Objects;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * This class extends the CommonsDaggerSupportFragment and creates leaderboard fragment
 */
public class LeaderboardFragment extends CommonsDaggerSupportFragment {


    @Inject
    SessionManager sessionManager;

    @Inject
    OkHttpJsonApiClient okHttpJsonApiClient;

    @Inject
    ViewModelFactory viewModelFactory;

    /**
     * View model for the paged leaderboard list
     */
    private LeaderboardListViewModel viewModel;

    /**
     * Composite disposable for API call
     */
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * Duration of the leaderboard API
     */
    private String duration;

    /**
     * Category of the Leaderboard API
     */
    private String category;

    /**
     * Page size of the leaderboard API
     */
    private int limit = PAGE_SIZE;

    /**
     * offset for the leaderboard API
     */
    private int offset = START_OFFSET;

    /**
     * Set initial User Rank to 0
     */
    private int userRank;

    /**
     * This variable represents if user wants to scroll to his rank or not
     */
    private boolean scrollToRank;

    private String userName;

    private FragmentLeaderboardBinding binding;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString(ProfileActivity.KEY_USERNAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLeaderboardBinding.inflate(inflater, container, false);

        hideLayouts();

        // Leaderboard currently unimplemented in Beta flavor. Skip all API calls and disable menu
        if(ConfigUtils.isBetaFlavour()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.scroll.setVisibility(View.GONE);
            return binding.getRoot();
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        setSpinners();

        /**
         * This array is for the duration filter, we have three filters weekly, yearly and all-time
         * each filter have a key and value pair, the value represents the param of the API
         */
        String[] durationValues = getContext().getResources().getStringArray(R.array.leaderboard_duration_values);

        /**
         * This array is for the category filter, we have three filters upload, used and nearby
         * each filter have a key and value pair, the value represents the param of the API
         */
        String[] categoryValues = getContext().getResources().getStringArray(R.array.leaderboard_category_values);

        duration = durationValues[0];
        category = categoryValues[0];

        setLeaderboard(duration, category, limit, offset);

        binding.durationSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                duration = durationValues[binding.durationSpinner.getSelectedItemPosition()];
                refreshLeaderboard();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        binding.categorySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                category = categoryValues[binding.categorySpinner.getSelectedItemPosition()];
                refreshLeaderboard();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


            binding.scroll.setOnClickListener(view -> scrollToUserRank());


        return binding.getRoot();
    }

    @Override
    public void setMenuVisibility(boolean visible) {
        super.setMenuVisibility(visible);

        // Whenever this fragment is revealed in a menu,
        // notify Beta users the page data is unavailable
        if(ConfigUtils.isBetaFlavour() && visible) {
            Context ctx = null;
            if(getContext() != null) {
                ctx = getContext();
            } else if(getView() != null && getView().getContext() != null) {
                ctx = getView().getContext();
            }
            if(ctx != null) {
                Toast.makeText(ctx,
                    R.string.leaderboard_unavailable_beta,
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Refreshes the leaderboard list
     */
    private void refreshLeaderboard() {
        scrollToRank = false;
        if (viewModel != null) {
            viewModel.refresh(duration, category, limit, offset);
            setLeaderboard(duration, category, limit, offset);
        }
    }

    /**
     * Performs Auto Scroll to the User's Rank
     * We use userRank+1 to load one extra user and prevent overlapping of my rank button
     * If you are viewing the leaderboard below userRank, it scrolls to the user rank at the top
     */
    private void scrollToUserRank() {

        if(userRank==0){
            Toast.makeText(getContext(),R.string.no_achievements_yet,Toast.LENGTH_SHORT).show();
        }else {
            if (binding == null) {
                return;
            }
            if (Objects.requireNonNull(binding.leaderboardList.getAdapter()).getItemCount()
                > userRank + 1) {
                binding.leaderboardList.smoothScrollToPosition(userRank + 1);
            } else {
                if (viewModel != null) {
                    viewModel.refresh(duration, category, userRank + 1, 0);
                    setLeaderboard(duration, category, userRank + 1, 0);
                    scrollToRank = true;
                }
            }
        }
              
    }

    /**
     * Set the spinners for the leaderboard filters
     */
    private void setSpinners() {
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(getContext(),
            R.array.leaderboard_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.categorySpinner.setAdapter(categoryAdapter);

        ArrayAdapter<CharSequence> durationAdapter = ArrayAdapter.createFromResource(getContext(),
            R.array.leaderboard_durations, android.R.layout.simple_spinner_item);
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.durationSpinner.setAdapter(durationAdapter);
    }

    /**
     * To call the API to get results
     * which then sets the views using setLeaderboardUser method
     */
    private void setLeaderboard(String duration, String category, int limit, int offset) {
        if (checkAccount()) {
            try {
                compositeDisposable.add(okHttpJsonApiClient
                    .getLeaderboard(Objects.requireNonNull(userName),
                        duration, category, null, null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        response -> {
                            if (response != null && response.getStatus() == 200) {
                                userRank = response.getRank();
                                setViews(response, duration, category, limit, offset);
                            }
                        },
                        t -> {
                            Timber.e(t, "Fetching leaderboard statistics failed");
                            onError();
                        }
                    ));
            }
            catch (Exception e){
                Timber.d(e+"success");
            }
        }
    }

    /**
     * Set the views
     * @param response Leaderboard Response Object
     */
    private void setViews(LeaderboardResponse response, String duration, String category, int limit, int offset) {
        viewModel = new ViewModelProvider(this, viewModelFactory).get(LeaderboardListViewModel.class);
        viewModel.setParams(duration, category, limit, offset);
        LeaderboardListAdapter leaderboardListAdapter = new LeaderboardListAdapter();
        UserDetailAdapter userDetailAdapter= new UserDetailAdapter(response);
        MergeAdapter mergeAdapter = new MergeAdapter(userDetailAdapter, leaderboardListAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        binding.leaderboardList.setLayoutManager(linearLayoutManager);
        binding.leaderboardList.setAdapter(mergeAdapter);
        viewModel.getListLiveData().observe(getViewLifecycleOwner(), leaderboardListAdapter::submitList);
        viewModel.getProgressLoadStatus().observe(getViewLifecycleOwner(), status -> {
            if (Objects.requireNonNull(status).equalsIgnoreCase(LOADING)) {
                showProgressBar();
            } else if (status.equalsIgnoreCase(LOADED)) {
                hideProgressBar();
                if (scrollToRank) {
                    binding.leaderboardList.smoothScrollToPosition(userRank + 1);
                }
            }
        });
    }

    /**
     * to hide progressbar
     */
    private void hideProgressBar() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.categorySpinner.setVisibility(View.VISIBLE);
            binding.durationSpinner.setVisibility(View.VISIBLE);
            binding.scroll.setVisibility(View.VISIBLE);
            binding.leaderboardList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * to show progressbar
     */
    private void showProgressBar() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.scroll.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * used to hide the layouts while fetching results from api
     */
    private void hideLayouts(){
        binding.categorySpinner.setVisibility(View.INVISIBLE);
        binding.durationSpinner.setVisibility(View.INVISIBLE);
        binding.leaderboardList.setVisibility(View.INVISIBLE);
    }

    /**
     * check to ensure that user is logged in
     * @return
     */
    private boolean checkAccount(){
        Account currentAccount = sessionManager.getCurrentAccount();
        if (currentAccount == null) {
            Timber.d("Current account is null");
            ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.user_not_logged_in));
            sessionManager.forceLogin(getActivity());
            return false;
        }
        return true;
    }

    /**
     * Shows a generic error toast when error occurs while loading leaderboard
     */
    private void onError() {
        ViewUtil.showLongToast(getActivity(), getResources().getString(R.string.error_occurred));
        if (binding!=null) {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        binding = null;
    }
}
