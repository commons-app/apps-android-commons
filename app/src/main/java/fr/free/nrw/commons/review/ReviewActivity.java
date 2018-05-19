package fr.free.nrw.commons.review;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AuthenticatedActivity;

/**
 * Created by root on 18.05.2018.
 */

public class ReviewActivity extends AuthenticatedActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @BindView(R.id.reviewPager)
    ViewPager pager;

    public static final int MAX_NUM = 4;
    private ReviewPagerAdapter reviewPagerAdapter;

    @Override
    protected void onAuthCookieAcquired(String authCookie) {

    }

    @Override
    protected void onAuthFailure() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        ButterKnife.bind(this);
        initDrawer();

        reviewPagerAdapter = new ReviewPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(reviewPagerAdapter);
        //pager.setAdapter(adapter);
        reviewPagerAdapter.getItem(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.review_randomizer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_review) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * References ReviewPagerAdapter to null before the activity is destroyed
     */
    @Override
    public void onDestroy() {
        //adapter.setCallback(null);
        super.onDestroy();
    }

    /**
     * Consumers should be simply using this method to use this activity.
     * @param context
     * @param title Page title
     * @param categoryName Name of the category for displaying its images
     */
    public static void startYourself(Context context, String title, String categoryName) {
        Intent reviewActivity = new Intent(context, ReviewActivity.class);
        context.startActivity(reviewActivity);
    }
/*
    @Override
    public void onYesClicked() {
        Log.d("deneme","onYesClicked");
    }

    @Override
    public void onNoClicked() {
        Log.d("deneme","onNoClicked");

    }

    @Override
    public void onNotSureClicked() {
        Log.d("deneme","onNotSureClicked");

    }*/
}
