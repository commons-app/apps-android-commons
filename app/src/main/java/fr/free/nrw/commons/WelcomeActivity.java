package fr.free.nrw.commons;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.CirclePageIndicator;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.theme.BaseActivity;

public class WelcomeActivity extends BaseActivity {

    @BindView(R.id.welcomePager) ViewPager pager;
    @BindView(R.id.welcomePagerIndicator) CirclePageIndicator indicator;

    private WelcomePagerAdapter adapter = new WelcomePagerAdapter();

    /**
     * Initialises exiting fields and dependencies
     *
     * @param savedInstanceState WelcomeActivity bundled data
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        ButterKnife.bind(this);

        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        adapter.setCallback(this::finish);
    }

    /**
     * References WelcomePageAdapter to null before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        adapter.setCallback(null);
        super.onDestroy();
    }
    
    /**
     * Creates a way to change current activity to WelcomeActivity
     *
     * @param context Activity context
     */
    public static void startYourself(Context context) {
        Intent welcomeIntent = new Intent(context, WelcomeActivity.class);
        context.startActivity(welcomeIntent);
    }
}
