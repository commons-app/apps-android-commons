package fr.free.nrw.commons;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.viewpagerindicator.CirclePageIndicator;

import fr.free.nrw.commons.theme.BaseActivity;

public class WelcomeActivity extends BaseActivity {
    private WelcomePagerAdapter adapter;

    @BindView(R.id.welcomePager) ViewPager pager;
    @BindView(R.id.welcomePagerIndicator) CirclePageIndicator indicator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_welcome, null);
        setContentView(view);

        getSupportActionBar().hide();
        ButterKnife.bind(this, view);

        setUpAdapter();
    }

    private void setUpAdapter() {
        adapter = new WelcomePagerAdapter(this);
        pager.setAdapter(adapter);
        indicator.setViewPager(pager);
    }
}
