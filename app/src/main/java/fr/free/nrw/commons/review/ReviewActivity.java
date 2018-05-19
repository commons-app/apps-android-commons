package fr.free.nrw.commons.review;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.mwapi.MediaResult;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

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

    @Inject MediaWikiApi mwApi;

    private ReviewPagerAdapter reviewPagerAdapter;

    //private ReviewCallback reviewCallback;
    private ReviewController reviewController;

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

        reviewController = new ReviewController();


        reviewPagerAdapter = new ReviewPagerAdapter(getSupportFragmentManager());
        pager.setAdapter(reviewPagerAdapter);
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

        if (id == R.id.action_review_randomizer) {
            Observable.fromCallable(() -> {
                try {
                    Media result = mwApi.getRecentRandomImage();
                    reviewController.onImageRefreshed(result.getFilename()); //file name is updated
                    reviewPagerAdapter.getItem(0); //new fragment with this new filename created

                    //String thumBaseUrl = Utils.makeThumbBaseUrl(result.getFilename());
                    //reviewPagerAdapter.currentThumbBasedUrl = thumBaseUrl;

                    //Log.d("review", result.getWikiSource());

                } catch (IOException e) {
                    Log.d("review", e.toString());
                }
                return "Booga!";
            })
                    .subscribeOn(Schedulers.io())
                    .subscribe();
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

    interface ReviewCallback {
        void onImageRefreshed(String itemTitle);
        void onQuestionChanged();
        void onSurveyFinished();
        void onImproperImageReported();
        void onLicenceViolationReported();
        void oWrongCategoryReported();
        void onThankSent();
    }
}
