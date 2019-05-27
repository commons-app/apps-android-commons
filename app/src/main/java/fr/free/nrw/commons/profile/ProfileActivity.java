package fr.free.nrw.commons.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.NavigationBaseActivity;

public class ProfileActivity extends NavigationBaseActivity {

    private FragmentManager supportFragmentManager;
    private ProfilePagerAdapter profilePagerAdapter;

    @BindView(R.id.viewPagerProfile)
    ViewPager viewPager;
    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        ButterKnife.bind(this);

        // Activity can call methods in the fragment by acquiring a
        // reference to the Fragment from FragmentManager, using findFragmentById()
        supportFragmentManager = getSupportFragmentManager();
        profilePagerAdapter = new ProfilePagerAdapter(supportFragmentManager, this);
        profilePagerAdapter = new ProfilePagerAdapter(supportFragmentManager, this);
        viewPager.setAdapter(profilePagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        setTitle(getString(R.string.title_activity_profile));
        initDrawer();
    }

    /**
     * Consumers should be simply using this method to use this activity.
     * @param context A Context of the application package implementing this class.
     */
    public static void startYourself(Context context) {
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
