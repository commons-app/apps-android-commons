package fr.free.nrw.commons;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        ButterKnife.bind(this);

        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
        adapter.setCallback(new WelcomePagerAdapter.Callback() {
            @Override
            public void onYesClicked() {
                finish();
            }
        });
    }

    @Override
    public void onDestroy() {
        adapter.setCallback(null);
        super.onDestroy();
    }
}
