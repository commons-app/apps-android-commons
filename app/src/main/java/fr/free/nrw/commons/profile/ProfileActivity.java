package fr.free.nrw.commons.profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.tabs.TabLayout;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.ViewPagerAdapter;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionsFragment;
import fr.free.nrw.commons.explore.ParentViewPager;
import fr.free.nrw.commons.profile.achievements.AchievementsFragment;
import fr.free.nrw.commons.profile.leaderboard.LeaderboardFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.DialogUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

/**
 * This activity will set two tabs, achievements and
 * each tab will have their own fragments
 */
public class ProfileActivity extends BaseActivity {

    private FragmentManager supportFragmentManager;

    @BindView(R.id.viewPager)
    ParentViewPager viewPager;

    @BindView(R.id.tab_layout)
    public TabLayout tabLayout;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Inject
    SessionManager sessionManager;

    private ViewPagerAdapter viewPagerAdapter;
    private AchievementsFragment achievementsFragment;
    private LeaderboardFragment leaderboardFragment;

    public static final String KEY_USERNAME ="username";
    public static final String KEY_SHOULD_SHOW_CONTRIBUTIONS ="shouldShowContributions";

    String userName;
    private boolean  shouldShowContributions;

    ContributionsFragment contributionsFragment;

    public void setScroll(boolean canScroll){
        viewPager.setCanScroll(canScroll);
    }
    @Override
    protected void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            userName = savedInstanceState.getString(KEY_USERNAME);
            shouldShowContributions = savedInstanceState.getBoolean(KEY_SHOULD_SHOW_CONTRIBUTIONS);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            onSupportNavigateUp();
        });

        userName = getIntent().getStringExtra(KEY_USERNAME);
        setTitle(userName);
        shouldShowContributions = getIntent().getBooleanExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, false);

        supportFragmentManager = getSupportFragmentManager();
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(viewPagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        setTabs();
    }

    /**
     * Navigate up event
     * @return boolean
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Creates a way to change current activity to AchievementActivity
     *
     * @param context
     */
    public static void startYourself(final Context context, final String userName,
        final boolean shouldShowContributions) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(KEY_USERNAME, userName);
        intent.putExtra(KEY_SHOULD_SHOW_CONTRIBUTIONS, shouldShowContributions);
        context.startActivity(intent);
    }

    /**
     * Set the tabs for the fragments
     */
    private void setTabs() {
        List<Fragment> fragmentList = new ArrayList<>();
        List<String> titleList = new ArrayList<>();
        achievementsFragment = new AchievementsFragment();
        Bundle achievementsBundle = new Bundle();
        achievementsBundle.putString(KEY_USERNAME, userName);
        achievementsFragment.setArguments(achievementsBundle);
        fragmentList.add(achievementsFragment);

        titleList.add(getResources().getString(R.string.achievements_tab_title).toUpperCase());
        leaderboardFragment = new LeaderboardFragment();
        Bundle leaderBoardBundle = new Bundle();
        leaderBoardBundle.putString(KEY_USERNAME, userName);
        leaderboardFragment.setArguments(leaderBoardBundle);

        fragmentList.add(leaderboardFragment);
        titleList.add(getResources().getString(R.string.leaderboard_tab_title).toUpperCase());

        contributionsFragment = new ContributionsFragment();
        Bundle contributionsListBundle = new Bundle();
        contributionsListBundle.putString(KEY_USERNAME, userName);
        contributionsFragment.setArguments(contributionsListBundle);
        fragmentList.add(contributionsFragment);
        titleList.add(getString(R.string.contributions_fragment).toUpperCase());

        viewPagerAdapter.setTabData(fragmentList, titleList);
        viewPagerAdapter.notifyDataSetChanged();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    /**
     * To inflate menu
     * @param menu Menu
     * @return boolean
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * To receive the id of selected item and handle further logic for that selected item
     * @param item MenuItem
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // take screenshot in form of bitmap and show it in Alert Dialog
        if (item.getItemId() == R.id.share_app_icon) {
            final View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            final Bitmap screenShot = Utils.getScreenShot(rootView);
            showAlert(screenShot);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * It displays the alertDialog with Image of screenshot
     * @param screenshot screenshot of the present screen
     */
    public void showAlert(final Bitmap screenshot) {
        final LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.image_alert_layout, null);
        final ImageView screenShotImage = view.findViewById(R.id.alert_image);
        screenShotImage.setImageBitmap(screenshot);
        final TextView shareMessage = view.findViewById(R.id.alert_text);
        shareMessage.setText(R.string.achievements_share_message);
        DialogUtil.showAlertDialog(this,
            null,
            null,
            getString(R.string.about_translate_proceed),
            getString(R.string.cancel),
            () -> shareScreen(screenshot),
            () -> {},
            view,
            true);
    }

    /**
     * To take bitmap and store it temporary storage and share it
     * @param bitmap bitmap of screenshot
     */
    void shareScreen(final Bitmap bitmap) {
        try {
            final File file = new File(getExternalCacheDir(), "screen.png");
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            file.setReadable(true, false);

            final Uri fileUri = FileProvider
                .getUriForFile(getApplicationContext(),
                    getPackageName() + ".provider", file);
            grantUriPermission(getPackageName(), fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setType("image/png");
            startActivity(Intent.createChooser(intent, getString(R.string.share_image_via)));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        outState.putString(KEY_USERNAME, userName);
        outState.putBoolean(KEY_SHOULD_SHOW_CONTRIBUTIONS, shouldShowContributions);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        // Checking if MediaDetailPagerFragment is visible, If visible then show ContributionListFragment else close the ProfileActivity
        if(contributionsFragment != null && contributionsFragment.getMediaDetailPagerFragment() != null && contributionsFragment.getMediaDetailPagerFragment().isVisible()) {
            contributionsFragment.backButtonClicked();
            tabLayout.setVisibility(View.VISIBLE);
        }else {
            super.onBackPressed();
        }
    }
}