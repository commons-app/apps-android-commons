package fr.free.nrw.commons.theme;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
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
import android.widget.Toast;

import butterknife.BindView;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.WelcomeActivity;
import fr.free.nrw.commons.auth.LoginActivity;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.nearby.NearbyActivity;
import fr.free.nrw.commons.settings.SettingsActivity;

public class NavigationBaseActivity extends BaseActivity
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
        // set width to lowerBound of 80% of the screen size
        params.width = (getResources().getDisplayMetrics().widthPixels * 70) / 100;
        navigationView.setLayoutParams(params);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_home:
                drawerLayout.closeDrawer(navigationView);
                if (!(this instanceof ContributionsActivity)) {
                    ContributionsActivity.startYourself(this);
                }
                return true;
            case R.id.action_nearby:
                drawerLayout.closeDrawer(navigationView);
                if (!(this instanceof NearbyActivity)) {
                    NearbyActivity.startYourself(this);
                }
                return true;
            case R.id.action_about:
                drawerLayout.closeDrawer(navigationView);
                if (!(this instanceof AboutActivity)) {
                    AboutActivity.startYourself(this);
                }
                return true;
            case R.id.action_settings:
                drawerLayout.closeDrawer(navigationView);
                if (!(this instanceof SettingsActivity)) {
                    SettingsActivity.startYourself(this);
                }
                return true;
            case R.id.action_introduction:
                drawerLayout.closeDrawer(navigationView);
                WelcomeActivity.startYourself(this);
                return true;
            case R.id.action_feedback:
                drawerLayout.closeDrawer(navigationView);
                Intent feedbackIntent = new Intent(Intent.ACTION_SEND);
                feedbackIntent.setType("message/rfc822");
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
            case R.id.action_developer_plans:
                drawerLayout.closeDrawer(navigationView);
                // Go to the page
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
                        .parse(getResources()
                                .getString(R.string.feedback_page_url)));
                startActivity(browserIntent);
                return true;
            case R.id.action_logout:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.logout_verification)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            ((CommonsApplication) getApplicationContext())
                                    .clearApplicationData(NavigationBaseActivity.this);
                            Intent nearbyIntent = new Intent(
                                    NavigationBaseActivity.this, LoginActivity.class);
                            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            nearbyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(nearbyIntent);
                            finish();
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                        .show();
                return true;
            default:
                return false;
        }
    }
}
