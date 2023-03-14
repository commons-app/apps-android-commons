package fr.free.nrw.commons.review;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.view.SimpleDraweeView;
import com.viewpagerindicator.CirclePageIndicator;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Inject;

public class ReviewActivity extends BaseActivity {

    @BindView(R.id.pager_indicator_review)
    public CirclePageIndicator pagerIndicator;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawerLayout;
    @BindView(R.id.view_pager_review)
    ReviewViewPager reviewPager;
    @BindView(R.id.skip_image)
    Button btnSkipImage;
    @BindView(R.id.review_image_view)
    SimpleDraweeView simpleDraweeView;
    @BindView(R.id.pb_review_image)
    ProgressBar progressBar;
    @BindView(R.id.tv_image_caption)
    TextView imageCaption;
    @BindView(R.id.mediaDetailContainer)
    FrameLayout mediaDetailContainer;
    MediaDetailFragment mediaDetailFragment;
    @BindView(R.id.reviewActivityContainer)
    LinearLayout reviewContainer;
    public ReviewPagerAdapter reviewPagerAdapter;
    public ReviewController reviewController;
    @Inject
    ReviewHelper reviewHelper;
    @Inject
    DeleteHelper deleteHelper;
    /**
     * Represent fragment for ReviewImage
     * Use to call some methods of ReviewImage fragment
     */
     private ReviewImageFragment reviewImageFragment;

    /**
     * Flag to check whether there are any non-hidden categories in the File
     */
    private boolean hasNonHiddenCategories = false;

    final String SAVED_MEDIA = "saved_media";
    private Media media;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (media != null) {
            outState.putParcelable(SAVED_MEDIA, media);
        }
    }

    /**
     * Consumers should be simply using this method to use this activity.
     *
     * @param context
     * @param title   Page title
     */
    public static void startYourself(Context context, String title) {
        Intent reviewActivity = new Intent(context, ReviewActivity.class);
        reviewActivity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        reviewActivity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(reviewActivity);
    }

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public Media getMedia() {
        return media;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        reviewController = new ReviewController(deleteHelper, this);

        reviewPagerAdapter = new ReviewPagerAdapter(getSupportFragmentManager());
        reviewPager.setAdapter(reviewPagerAdapter);
        pagerIndicator.setViewPager(reviewPager);
        progressBar.setVisibility(View.VISIBLE);

        Drawable d[]=btnSkipImage.getCompoundDrawablesRelative();
        d[2].setColorFilter(getApplicationContext().getResources().getColor(R.color.button_blue), PorterDuff.Mode.SRC_IN);

        if (savedInstanceState != null && savedInstanceState.getParcelable(SAVED_MEDIA) != null) {
            updateImage(savedInstanceState.getParcelable(SAVED_MEDIA)); // Use existing media if we have one
            setUpMediaDetailOnOrientation();
        } else {
            runRandomizer(); //Run randomizer whenever everything is ready so that a first random image will be added
        }

        btnSkipImage.setOnClickListener(view -> {
            reviewImageFragment = getInstanceOfReviewImageFragment();
            reviewImageFragment.disableButtons();
            runRandomizer();
        });

        simpleDraweeView.setOnClickListener(view ->setUpMediaDetailFragment());

        btnSkipImage.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && event.getRawX() >= (
                    btnSkipImage.getRight() - btnSkipImage
                            .getCompoundDrawables()[2].getBounds().width())) {
                showSkipImageInfo();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @SuppressLint("CheckResult")
    public boolean runRandomizer() {
        hasNonHiddenCategories = false;
        progressBar.setVisibility(View.VISIBLE);
        reviewPager.setCurrentItem(0);
        // Finds non-hidden categories from Media instance
        compositeDisposable.add(reviewHelper.getRandomMedia()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::checkWhetherFileIsUsedInWikis));
        return true;
    }

    /**
     * Check whether media is used or not in any Wiki Page
     */
    @SuppressLint("CheckResult")
    private void checkWhetherFileIsUsedInWikis(final Media media) {
        compositeDisposable.add(reviewHelper.checkFileUsage(media.getFilename())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(result -> {
                // result false indicates media is not used in any wiki
                if (!result) {
                    // Finds non-hidden categories from Media instance
                    findNonHiddenCategories(media);
                } else {
                    runRandomizer();
                }
            }));
    }

    /**
     * Finds non-hidden categories and updates current image
     */
    private void findNonHiddenCategories(Media media) {
        for(String key : media.getCategoriesHiddenStatus().keySet()) {
            Boolean value = media.getCategoriesHiddenStatus().get(key);
            // If non-hidden category is found then set hasNonHiddenCategories to true
            // so that category review cannot be skipped
            if(!value) {
                hasNonHiddenCategories = true;
                break;
            }
        }
        reviewImageFragment = getInstanceOfReviewImageFragment();
        reviewImageFragment.disableButtons();
        updateImage(media);
    }

    @SuppressLint("CheckResult")
    private void updateImage(Media media) {
        reviewHelper.addViewedImagesToDB(media.getPageId());
        this.media = media;
        String fileName = media.getFilename();
        if (fileName.length() == 0) {
            ViewUtil.showShortSnackbar(drawerLayout, R.string.error_review);
            return;
        }

        simpleDraweeView.setImageURI(media.getImageUrl());

        reviewController.onImageRefreshed(media); //file name is updated
        compositeDisposable.add(reviewHelper.getFirstRevisionOfFile(fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(revision -> {
                    reviewController.firstRevision = revision;
                    reviewPagerAdapter.updateFileInformation();
                    @SuppressLint({"StringFormatInvalid", "LocalSuppress"}) String caption = String.format(getString(R.string.review_is_uploaded_by), fileName, revision.getUser());
                    imageCaption.setText(caption);
                    progressBar.setVisibility(View.GONE);
                    reviewImageFragment = getInstanceOfReviewImageFragment();
                    reviewImageFragment.enableButtons();
                }));
        reviewPager.setCurrentItem(0);
    }

    public void swipeToNext() {
        int nextPos = reviewPager.getCurrentItem() + 1;
        // If currently at category fragment, then check whether the media has any non-hidden category
        if (nextPos <= 3) {
            reviewPager.setCurrentItem(nextPos);
            if (nextPos == 2) {
                // The media has no non-hidden category. Such media are already flagged by server-side bots, so no need to review manually.
                if (!hasNonHiddenCategories) {
                    swipeToNext();
                    return;
                }
            }
        } else {
            runRandomizer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    public void showSkipImageInfo(){
        DialogUtil.showAlertDialog(ReviewActivity.this,
                getString(R.string.skip_image).toUpperCase(),
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_review_activty, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_image_info:
                showReviewImageInfo();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * this function return the instance of  reviewImageFragment
     */
    public ReviewImageFragment getInstanceOfReviewImageFragment(){
        int currentItemOfReviewPager = reviewPager.getCurrentItem();
        reviewImageFragment = (ReviewImageFragment) reviewPagerAdapter.instantiateItem(reviewPager, currentItemOfReviewPager);
        return reviewImageFragment;
    }

    /**
     * set up the media detail fragment when click on the review image
     */
    private void setUpMediaDetailFragment() {
        if (mediaDetailContainer.getVisibility() == View.GONE && media != null) {
            mediaDetailContainer.setVisibility(View.VISIBLE);
            reviewContainer.setVisibility(View.INVISIBLE);
            FragmentManager fragmentManager = getSupportFragmentManager();
            mediaDetailFragment = new MediaDetailFragment();
            Bundle bundle = new Bundle();
            bundle.putParcelable("media", media);
            mediaDetailFragment.setArguments(bundle);
            fragmentManager.beginTransaction().add(R.id.mediaDetailContainer, mediaDetailFragment).
                addToBackStack("MediaDetail").commit();
        }
    }

    /**
     * handle the back pressed event of this activity
     * this function call every time when back button is pressed
     */
    @Override
    public void onBackPressed() {
        if (mediaDetailContainer.getVisibility() == View.VISIBLE) {
            mediaDetailContainer.setVisibility(View.GONE);
            reviewContainer.setVisibility(View.VISIBLE);
        }
        super.onBackPressed();
    }

    /**
     * set up media detail fragment after orientation change
     */
    private void setUpMediaDetailOnOrientation() {
        Fragment mediaDetailFragment = getSupportFragmentManager()
            .findFragmentById(R.id.mediaDetailContainer);
        if (mediaDetailFragment != null) {
            mediaDetailContainer.setVisibility(View.VISIBLE);
            reviewContainer.setVisibility(View.INVISIBLE);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.mediaDetailContainer, mediaDetailFragment).commit();
        }
    }
}
