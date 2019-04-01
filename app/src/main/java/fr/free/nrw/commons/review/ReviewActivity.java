package fr.free.nrw.commons.review;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.navigation.NavigationView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.ArrayList;

import javax.inject.Inject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.achievements.AchievementsActivity;
import fr.free.nrw.commons.auth.AuthenticatedActivity;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.MediaDataExtractorUtil;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ReviewActivity extends AuthenticatedActivity {

    @BindView(R.id.reviewPagerIndicator)
    public CirclePageIndicator pagerIndicator;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.navigation_view)
    NavigationView navigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.reviewPager)
    ReviewViewPager reviewPager;
    @BindView(R.id.skip_image)
    Button skip_image_button;
    @BindView(R.id.imageView)
    SimpleDraweeView simpleDraweeView;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.imageCaption)
    TextView imageCaption;
    @BindView(R.id.skip_image_info)
    ImageView skipImageInfo;
    @BindView(R.id.review_image_info)
    ImageView reviewImageInfo;
    public ReviewPagerAdapter reviewPagerAdapter;
    public ReviewController reviewController;
    @Inject
    MediaWikiApi mwApi;
    @Inject
    ReviewHelper reviewHelper;
    @Inject
    DeleteHelper deleteHelper;

    /**
     * Consumers should be simply using this method to use this activity.
     *
     * @param context
     * @param title   Page title
     */
    public static void startYourself(Context context, String title) {
        Intent reviewActivity = new Intent(context, ReviewActivity.class);
        context.startActivity(reviewActivity);
    }

    private CompositeDisposable compositeDisposable = new CompositeDisposable();


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

        reviewController = new ReviewController(deleteHelper, this);

        reviewPagerAdapter = new ReviewPagerAdapter(getSupportFragmentManager());
        reviewPager.setAdapter(reviewPagerAdapter);
        reviewPagerAdapter.getItem(0);
        pagerIndicator.setViewPager(reviewPager);
        progressBar.setVisibility(View.VISIBLE);

        runRandomizer(); //Run randomizer whenever everything is ready so that a first random image will be added

        skip_image_button.setOnClickListener(view -> runRandomizer());
        skipImageInfo.setOnClickListener(view -> showSkipImageInfo());
        reviewImageInfo.setOnClickListener(view -> showReviewImageInfo());
    }

    @SuppressLint("CheckResult")
    public boolean runRandomizer() {
        progressBar.setVisibility(View.VISIBLE);
        reviewPager.setCurrentItem(0);
        compositeDisposable.add(reviewHelper.getRandomMedia()
                .map(Media::getFilename)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateImage));
        return true;
    }

    @SuppressLint("CheckResult")
    private void updateImage(String fileName) {
        if (fileName.length() == 0) {
            ViewUtil.showShortSnackbar(drawerLayout, R.string.error_review);
            return;
        }
        simpleDraweeView.setImageURI(Utils.makeThumbBaseUrl(fileName));
        reviewController.onImageRefreshed(fileName); //file name is updated
        compositeDisposable.add(reviewHelper.getFirstRevisionOfFile("File:" + fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revision -> {
                    reviewController.firstRevision = revision;
                    reviewPagerAdapter.updateFileInformation(fileName);
                    ((TextView) imageCaption).setText(fileName + " is uploaded by: " + revision.content());
                    progressBar.setVisibility(View.GONE);
                }));
        reviewPager.setCurrentItem(0);

        Disposable disposable = mwApi.fetchMediaByFilename("File:" + fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaResult -> {
                    ArrayList<String> categories = MediaDataExtractorUtil.extractCategories(mediaResult.getWikiSource());
                    updateCategories(categories);
                }, this::categoryFetchError);
        compositeDisposable.add(disposable);
    }

    private void categoryFetchError(Throwable throwable) {
        Timber.e(throwable, "Error fetching categories");
        ViewUtil.showShortSnackbar(drawerLayout, R.string.error_review_categories);
    }

    private void updateCategories(ArrayList<String> categories) {
        reviewController.onCategoriesRefreshed(categories);
        reviewPagerAdapter.updateCategories();
    }

    public void swipeToNext() {
        int nextPos = reviewPager.getCurrentItem() + 1;
        if (nextPos <= 3) {
            reviewPager.setCurrentItem(nextPos);
        } else {
            runRandomizer();
        }
    }

    public void showSkipImageInfo(){
        DialogUtil.showAlertDialog(ReviewActivity.this,
                getString(R.string.skip_image),
                getString(R.string.skip_image_explanation),
                getString(android.R.string.ok),
                "",
                null,
                null);
    }

    public void showReviewImageInfo() {
        DialogUtil.showAlertDialog(ReviewActivity.this,
                getString(R.string.title_activity_review),
                getString(R.string.review_image_explanation),
                getString(android.R.string.ok),
                "",
                null,
                null);
    }
}
