package fr.free.nrw.commons.hamburger;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.R;


public class NavigationBaseFragment extends Fragment {
    @BindView(R.id.home_item)
    TextView homeItem;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View hamburgerView = inflater.inflate(R.layout.navigation_drawer_menu, container, false);
        ButterKnife.bind(this, hamburgerView);
        setupHamburgerMenu();
        return hamburgerView;
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

    @OnClick(R.id.home_item)
    protected void onProfileLayoutClick() {
        closeDrawer();
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
