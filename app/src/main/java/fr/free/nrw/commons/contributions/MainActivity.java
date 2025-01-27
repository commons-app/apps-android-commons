package fr.free.nrw.commons.contributions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.work.ExistingWorkPolicy;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import fr.free.nrw.commons.databinding.FragmentContributionsListBinding;
import fr.free.nrw.commons.databinding.MainBinding;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.bookmarks.BookmarkFragment;
import fr.free.nrw.commons.explore.ExploreFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.media.MediaDetailPagerFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetFragment;
import fr.free.nrw.commons.navtab.MoreBottomSheetLoggedOutFragment;
import fr.free.nrw.commons.navtab.NavTab;
import fr.free.nrw.commons.navtab.NavTabLayout;
import fr.free.nrw.commons.navtab.NavTabLoggedOut;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.notification.NotificationController;
import fr.free.nrw.commons.profile.ProfileActivity;
import fr.free.nrw.commons.quiz.QuizChecker;
import fr.free.nrw.commons.settings.SettingsFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.upload.UploadProgressActivity;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtilWrapper;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import kotlin.Unit;
import org.apache.commons.lang3.StringUtils;
import timber.log.Timber;

public class MainActivity extends BaseActivity
    implements OnBackStackChangedListener {

    @Inject
    SessionManager sessionManager;
    @Inject
    ContributionController controller;
    @Inject
    ContributionDao contributionDao;

    @Inject
    ContributionsListPresenter contributionsListPresenter;
    @Inject
    ContributionsRemoteDataSource dataSource;

    private ContributionsFragment contributionsFragment;
    private NearbyParentFragment nearbyParentFragment;
    private ExploreFragment exploreFragment;
    private BookmarkFragment bookmarkFragment;
    public ActiveFragment activeFragment;
    private MediaDetailPagerFragment mediaDetailPagerFragment;
    private OnNavigationItemSelectedListener navListener;

    @Inject
    public LocationServiceManager locationManager;
    @Inject
    NotificationController notificationController;
    @Inject
    QuizChecker quizChecker;
    @Inject
    @Named("default_preferences")
    public JsonKvStore applicationKvStore;
    @Inject
    ViewUtilWrapper viewUtilWrapper;

    public Menu menu;

    public MainBinding binding;

    NavTabLayout tabLayout;

    private FragmentContributionsListBinding refresh;

    private String userName;

    /**
     * Consumers should be simply using this method to use this activity.
     *
     * @param context A Context of the application package implementing this class.
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            if (!contributionsFragment.backButtonClicked()) {
                return false;
            }
        } else {
            onBackPressed();
            showTabs();
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbarBinding.toolbar);
        tabLayout = binding.fragmentMainNavTabLayout;
        loadLocale();

        binding.toolbarBinding.toolbar.setNavigationOnClickListener(view -> {
            onSupportNavigateUp();
        });

        applicationKvStore.putBoolean("first_edit_depict", true);
        if (applicationKvStore.getBoolean("login_skipped") == true) {
            setTitle(getString(R.string.navigation_item_explore));
            setUpLoggedOutPager();
        } else {
            if (applicationKvStore.getBoolean("firstrun", true)) {
                applicationKvStore.putBoolean("hasAlreadyLaunchedBigMultiupload", false);
                applicationKvStore.putBoolean("hasAlreadyLaunchedCategoriesDialog", false);
            }
            if (savedInstanceState == null) {
                if (applicationKvStore.getBoolean("last_opened_nearby")) {
                    setTitle(getString(R.string.nearby_fragment));
                    showNearby();
                    loadFragment(NearbyParentFragment.newInstance(), false);
                } else {
                    setTitle(getString(R.string.contributions_fragment));
                    loadFragment(ContributionsFragment.newInstance(), false);
                }
            }
            refresh = FragmentContributionsListBinding.inflate(getLayoutInflater());
            if (getIntent().getExtras() != null) {
                userName = getIntent().getExtras().getString(ProfileActivity.KEY_USERNAME);
            }

            if (StringUtils.isEmpty(userName)) {
                userName = sessionManager.getUserName();
            }
            setUpPager();
            checkAndResumeStuckUploads();
        }
    }

    public void setSelectedItemId(int id) {
        binding.fragmentMainNavTabLayout.setSelectedItemId(id);
    }

    private void setUpPager() {
        binding.fragmentMainNavTabLayout.setOnNavigationItemSelectedListener(
            navListener = (item) -> {
                if (!item.getTitle().equals(getString(R.string.more))) {
                    setTitle(item.getTitle());
                }
                applicationKvStore.putBoolean("last_opened_nearby",
                    item.getTitle().equals(getString(R.string.nearby_fragment)));
                final Fragment fragment = NavTab.of(item.getOrder()).newInstance();
                return loadFragment(fragment, true);
            });
    }

    private void setUpLoggedOutPager() {
        loadFragment(ExploreFragment.newInstance(), false);
        binding.fragmentMainNavTabLayout.setOnNavigationItemSelectedListener(item -> {
            if (!item.getTitle().equals(getString(R.string.more))) {
                setTitle(item.getTitle());
            }
            Fragment fragment = NavTabLoggedOut.of(item.getOrder()).newInstance();
            return loadFragment(fragment, true);
        });
    }

    private boolean loadFragment(Fragment fragment, boolean showBottom) {
        if (fragment instanceof ContributionsFragment) {
            if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
                contributionsFragment.scrollToTop();
                return true;
            }
            contributionsFragment = (ContributionsFragment) fragment;
            activeFragment = ActiveFragment.CONTRIBUTIONS;
        } else if (fragment instanceof NearbyParentFragment) {
            if (activeFragment == ActiveFragment.NEARBY) {
                return true;
            }
            nearbyParentFragment = (NearbyParentFragment) fragment;
            activeFragment = ActiveFragment.NEARBY;
        } else if (fragment instanceof ExploreFragment) {
            if (activeFragment == ActiveFragment.EXPLORE) {
                return true;
            }
            exploreFragment = (ExploreFragment) fragment;
            activeFragment = ActiveFragment.EXPLORE;
        } else if (fragment instanceof BookmarkFragment) {
            if (activeFragment == ActiveFragment.BOOKMARK) {
                return true;
            }
            bookmarkFragment = (BookmarkFragment) fragment;
            activeFragment = ActiveFragment.BOOKMARK;
        } else if (fragment == null && showBottom) {
            if (applicationKvStore.getBoolean("login_skipped") == true) {
                MoreBottomSheetLoggedOutFragment bottomSheet = new MoreBottomSheetLoggedOutFragment();
                bottomSheet.show(getSupportFragmentManager(), "MoreBottomSheetLoggedOut");
            } else {
                MoreBottomSheetFragment bottomSheet = new MoreBottomSheetFragment();
                bottomSheet.show(getSupportFragmentManager(), "MoreBottomSheet");
            }
        }

        if (fragment != null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
            return true;
        }
        return false;
    }

    public void hideTabs() {
        binding.fragmentMainNavTabLayout.setVisibility(View.GONE);
    }

    public void showTabs() {
        binding.fragmentMainNavTabLayout.setVisibility(View.VISIBLE);
    }

    public void setNumOfUploads(int uploadCount) {
        if (activeFragment == ActiveFragment.CONTRIBUTIONS) {
            setTitle(getResources().getString(R.string.contributions_fragment) + " " + (
                !(uploadCount == 0) ?
                    getResources()
                        .getQuantityString(R.plurals.contributions_subtitle,
                            uploadCount, uploadCount)
                    : getString(R.string.contributions_subtitle_zero)));
        }
    }

    @SuppressLint("CheckResult")
    private void checkAndResumeStuckUploads() {
        List<Contribution> stuckUploads = contributionDao.getContribution(
                Collections.singletonList(Contribution.STATE_IN_PROGRESS))
            .subscribeOn(Schedulers.io())
            .blockingGet();
        Timber.d("Resuming " + stuckUploads.size() + " uploads...");
        if (!stuckUploads.isEmpty()) {
            for (Contribution contribution : stuckUploads) {
                contribution.setState(Contribution.STATE_QUEUED);
                contribution.setDateUploadStarted(Calendar.getInstance().getTime());
                Completable.fromAction(() -> contributionDao.saveSynchronous(contribution))
                    .subscribeOn(Schedulers.io())
                    .subscribe();
            }
            WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                this, ExistingWorkPolicy.APPEND_OR_REPLACE);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("viewPagerCurrentItem", binding.pager.getCurrentItem());
        outState.putString("activeFragment", activeFragment.name());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String activeFragmentName = savedInstanceState.getString("activeFragment");
        if (activeFragmentName != null) {
            restoreActiveFragment(activeFragmentName);
        }
    }

    private void restoreActiveFragment(@NonNull String fragmentName) {
        if (fragmentName.equals(ActiveFragment.CONTRIBUTIONS.name())) {
            setTitle(getString(R.string.contributions_fragment));
            loadFragment(ContributionsFragment.newInstance(), false);
        } else if (fragmentName.equals(ActiveFragment.NEARBY.name())) {
            setTitle(getString(R.string.nearby_fragment));
            loadFragment(NearbyParentFragment.newInstance(), false);
        } else if (fragmentName.equals(ActiveFragment.EXPLORE.name())) {
            setTitle(getString(R.string.navigation_item_explore));
            loadFragment(ExploreFragment.newInstance(), false);
        } else if (fragmentName.equals(ActiveFragment.BOOKMARK.name())) {
            setTitle(getString(R.string.bookmarks));
            loadFragment(BookmarkFragment.newInstance(), false);
        }
    }

    @Override
    public void onBackPressed() {
        if (contributionsFragment != null && activeFragment == ActiveFragment.CONTRIBUTIONS) {
            if (!contributionsFragment.backButtonClicked()) {
                super.onBackPressed();
            }
        } else if (nearbyParentFragment != null && activeFragment == ActiveFragment.NEARBY) {
            if (!nearbyParentFragment.backButtonClicked()) {
                getSupportFragmentManager().beginTransaction().remove(nearbyParentFragment)
                    .commit();
                setSelectedItemId(NavTab.CONTRIBUTIONS.code());
            }
        } else if (exploreFragment != null && activeFragment == ActiveFragment.EXPLORE) {
            if (!exploreFragment.onBackPressed()) {
                if (applicationKvStore.getBoolean("login_skipped")) {
                    super.onBackPressed();
                } else {
                    setSelectedItemId(NavTab.CONTRIBUTIONS.code());
                }
            }
        } else if (bookmarkFragment != null && activeFragment == ActiveFragment.BOOKMARK) {
            bookmarkFragment.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onBackStackChanged() {
    }

    @SuppressLint("CheckResult")
    private void retryAllFailedUploads() {
        contributionDao.
            getContribution(Collections.singletonList(Contribution.STATE_FAILED))
            .subscribeOn(Schedulers.io())
            .subscribe(failedUploads -> {
                for (Contribution contribution : failedUploads) {
                    contributionsFragment.retryUpload(contribution);
                }
            });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upload_tab:
                startActivity(new Intent(this, UploadProgressActivity.class));
                return true;
            case R.id.notifications:
                NotificationActivity.Companion.startYourself(this, "unread");
                return true;
            case R.id.menu_refresh:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contribution_activity_notification_menu, menu);

        MenuItem refreshItem = menu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            View actionView = refreshItem.getActionView();
            if (actionView != null) {
                ImageView refreshIcon = actionView.findViewById(R.id.refresh_icon);
                if (refreshIcon != null) {
                    refreshIcon.setOnClickListener(v -> {
                        v.clearAnimation();
                        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate);
                        v.startAnimation(rotateAnimation);

                        // Initialize userName if it's null
                        if (userName == null) {
                            userName = sessionManager.getUserName();
                        }

                        if (Objects.equals(sessionManager.getUserName(), userName)) {
                            if (refresh != null && refresh.swipeRefreshLayout != null) {
                                refresh.swipeRefreshLayout.setRefreshing(true);
                                refresh.swipeRefreshLayout.setEnabled(true);
                                contributionsListPresenter.refreshList(refresh.swipeRefreshLayout);
                            }
                        } else {
                            if (refresh != null && refresh.swipeRefreshLayout != null) {
                                refresh.swipeRefreshLayout.setEnabled(false);
                            }
                        }
                    });
                }
            }
        }

        return true;
    }

    public void centerMapToPlace(Place place) {
        setSelectedItemId(NavTab.NEARBY.code());
        nearbyParentFragment.setNearbyParentFragmentInstanceReadyCallback(
            new NearbyParentFragmentInstanceReadyCallback() {
                @Override
                public void onReady() {
                    nearbyParentFragment.centerMapToPlace(place);
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (applicationKvStore.getBoolean("firstrun", true) &&
            !applicationKvStore.getBoolean("login_skipped")) {
            defaultKvStore.putBoolean("inAppCameraFirstRun", true);
            WelcomeActivity.startYourself(this);
        }

        retryAllFailedUploads();
        //check for new contributions
        // Initialize userName if it's null
        if (userName == null) {
            userName = sessionManager.getUserName();
        }

        if (Objects.equals(sessionManager.getUserName(), userName)) {
            if (refresh != null && refresh.swipeRefreshLayout != null) {
                refresh.swipeRefreshLayout.setRefreshing(true);
                refresh.swipeRefreshLayout.setEnabled(true);
                contributionsListPresenter.refreshList(refresh.swipeRefreshLayout);
            }
        } else {
            if (refresh != null && refresh.swipeRefreshLayout != null) {
                refresh.swipeRefreshLayout.setEnabled(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        quizChecker.cleanup();
        locationManager.unregisterLocationManager();
        locationManager = null;
        super.onDestroy();
    }

    public void showNearby() {
        binding.fragmentMainNavTabLayout.setSelectedItemId(NavTab.NEARBY.code());
    }

    public enum ActiveFragment {
        CONTRIBUTIONS,
        NEARBY,
        EXPLORE,
        BOOKMARK,
        MORE
    }

    private void loadLocale() {
        final SharedPreferences preferences = getSharedPreferences("Settings",
            Activity.MODE_PRIVATE);
        final String language = preferences.getString("language", "");
        final SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setLocale(this, language);
    }

    public OnNavigationItemSelectedListener getNavListener() {
        return navListener;
    }
}