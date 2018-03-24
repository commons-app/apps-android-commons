package fr.free.nrw.commons.theme;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.featured.FeaturedImagesActivity;
import fr.free.nrw.commons.nearby.NearbyActivity;
import fr.free.nrw.commons.notification.NotificationActivity;
import fr.free.nrw.commons.settings.SettingsActivity;
import timber.log.Timber;

public abstract class NavigationBaseActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    private ActionBarDrawerToggle toggle;

    public void initDrawer() {
        navigationView.setNavigationItemSelectedListener(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
        setDrawerPaneWidth();
        setUserName();
    }

    /**
     * Set the username in navigationHeader.
     */
    private void setUserName() {

        View navHeaderView = navigationView.getHeaderView(0);
        TextView username = navHeaderView.findViewById(R.id.username);

        AccountManager accountManager = AccountManager.get(this);
        Account[] allAccounts = accountManager.getAccountsByType(AccountUtil.ACCOUNT_TYPE);
        if (allAccounts.length != 0) {
            username.setText(allAccounts[0].name);
        }
    }

    public void initBackButton() {
        int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
        toggle.setDrawerIndicatorEnabled(backStackEntryCount == 0);
        toggle.setToolbarNavigationClickListener(v -> onBackPressed());
    }

    public void initBack() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setDrawerPaneWidth() {
        ViewGroup.LayoutParams params = navigationView.getLayoutParams();
        // set width to lowerBound of 70% of the screen size in portrait mode
        // set width to lowerBound of 50% of the screen size in landscape mode
        int percentageWidth = getResources().getInteger(R.integer.drawer_width);

        params.width = (getResources().getDisplayMetrics().widthPixels * percentageWidth) / 100;
        navigationView.setLayoutParams(params);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_home:
                drawerLayout.closeDrawer(navigationView);
                startActivityWithFlags(
                        this, ContributionsActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                return true;
            case R.id.action_nearby:
                drawerLayout.closeDrawer(navigationView);
                startActivityWithFlags(this, NearbyActivity.class, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                return true;
            case R.id.action_about:
                drawerLayout.closeDrawer(navigationView);
                startActivityWithFlags(this, AboutActivity.class, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                return true;
            case R.id.action_settings:
                drawerLayout.closeDrawer(navigationView);
                startActivityWithFlags(this, SettingsActivity.class, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                return true;
            case R.id.action_introduction:
                drawerLayout.closeDrawer(navigationView);
                WelcomeActivity.startYourself(this);
                return true;
            case R.id.action_feedback:
                drawerLayout.closeDrawer(navigationView);
                Intent feedbackIntent = new Intent(Intent.ACTION_SENDTO);
                feedbackIntent.setType("message/rfc822");
                feedbackIntent.setData(Uri.parse("mailto:"));
                feedbackIntent.putExtra(Intent.EXTRA_EMAIL,
                        new String[]{CommonsApplication.FEEDBACK_EMAIL});
                feedbackIntent.putExtra(Intent.EXTRA_SUBJECT,
                        String.format(CommonsApplication.FEEDBACK_EMAIL_SUBJECT,
                                BuildConfig.VERSION_NAME));
                try {
                    startActivity(feedbackIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_email_client, Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_logout:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.logout_verification)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            BaseLogoutListener logoutListener = new BaseLogoutListener();
                            CommonsApplication app = (CommonsApplication) getApplication();
                            app.clearApplicationData(this, logoutListener);
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                        .show();
                return true;
            case R.id.action_notifications:
                drawerLayout.closeDrawer(navigationView);
                NotificationActivity.startYourself(this);
                return true;
            case R.id.action_featured_images:
                drawerLayout.closeDrawer(navigationView);
                startActivityWithFlags(this, FeaturedImagesActivity.class, Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                return true;
            default:
                Timber.e("Unknown option [%s] selected from the navigation menu", itemId);
                return false;
        }
    }

    private class BaseLogoutListener implements CommonsApplication.LogoutListener {
        @Override
        public void onLogoutComplete() {
            Timber.d("Logout complete callback received.");
            Intent nearbyIntent = new Intent(
                    NavigationBaseActivity.this, LoginActivity.class);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(nearbyIntent);
            finish();
        }
    }

    public static <T> void startActivityWithFlags(Context context, Class<T> cls, int... flags) {
        Intent intent = new Intent(context, cls);
        for (int flag: flags) {
            intent.addFlags(flag);
        }
        context.startActivity(intent);
    }
}
