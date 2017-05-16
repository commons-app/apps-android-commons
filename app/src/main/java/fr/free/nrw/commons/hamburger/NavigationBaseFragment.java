package fr.free.nrw.commons.hamburger;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.AboutActivity;
import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.ContributionsActivity;
import fr.free.nrw.commons.nearby.NearbyActivity;
import fr.free.nrw.commons.settings.SettingsActivity;


public class NavigationBaseFragment extends Fragment {
    @BindView(R.id.pictureOfTheDay)
    ImageView pictureOfTheDay;

    @BindView(R.id.upload_item)
    TextView uploadItem;

    @BindView(R.id.nearby_item)
    TextView nearbyItem;

    @BindView(R.id.about_item)
    TextView aboutItem;

    @BindView(R.id.settings_item)
    TextView settingsItem;

    @BindView(R.id.feedback_item)
    TextView feedbackItem;

    @BindView(R.id.logout_item)
    TextView logoutItem;

    private DrawerLayout drawerLayout;
    private RelativeLayout drawerPane;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View hamburgerView = inflater.inflate(R.layout.navigation_drawer_menu, container, false);
        ButterKnife.bind(this, hamburgerView);
        showPictureOfTheDay();
        setupHamburgerMenu();
        return hamburgerView;
    }

    private void showPictureOfTheDay() {
        pictureOfTheDay.setImageDrawable(getResources().getDrawable(R.drawable.commons_logo_large));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupHamburgerMenu() {
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(getActivity(),
                drawerLayout, R.string.ok, R.string.cancel);
        if (getActivity() instanceof HamburgerMenuContainer) {
            ((HamburgerMenuContainer) getActivity()).setDrawerListener(drawerToggle);
        }
    }

    @OnClick(R.id.upload_item)
    protected void onUploadItemClicked() {
        closeDrawer();
        ContributionsActivity.startYourself(getActivity());
    }

    @OnClick(R.id.settings_item)
    protected void onSettingsItemClicked() {
        closeDrawer();
        SettingsActivity.startYourself(getActivity());
    }

    @OnClick(R.id.about_item)
    protected void onAboutItemClicked() {
        closeDrawer();
        AboutActivity.startYourself(getActivity());
    }

    @OnClick(R.id.nearby_item)
    protected void onNearbyItemClicked() {
        closeDrawer();
        NearbyActivity.startYourself(getActivity());
    }

    @OnClick(R.id.feedback_item)
    protected void onFeedbackItemClicked() {
        closeDrawer();
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
            Toast.makeText(getActivity(), R.string.no_email_client, Toast.LENGTH_SHORT).show();
        }
    }

    private void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(drawerPane);
        }
    }

    public void setDrawerLayout(DrawerLayout drawerLayout, RelativeLayout drawerPane) {
        this.drawerLayout = drawerLayout;
        this.drawerPane = drawerPane;
    }
}
