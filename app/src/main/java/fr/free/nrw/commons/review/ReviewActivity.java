package fr.free.nrw.commons.review;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.auth.AccountUtil;
import fr.free.nrw.commons.databinding.ActivityReviewBinding;
import fr.free.nrw.commons.delete.DeleteHelper;
import fr.free.nrw.commons.media.MediaDetailFragment;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

public class ReviewActivity extends BaseActivity {

    private ActivityReviewBinding binding;

    MediaDetailFragment mediaDetailFragment;
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

    private List<Media> cachedMedia = new ArrayList<>();

    /** Constants for managing media cache in the review activity */
    // Name of SharedPreferences file for storing review activity preferences
    private static final String PREF_NAME = "ReviewActivityPrefs";
    // Key for storing the timestamp of last cache update
    private static final String LAST_CACHE_TIME_KEY = "lastCacheTime";
    // Maximum number of media files to store in cache
    private static final int CACHE_SIZE = 5;
    // Cache expiration time in milliseconds (24 hours)
    private static final long CACHE_EXPIRY_TIME = 24 * 60 * 60 * 1000;

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
        binding = ActivityReviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarBinding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        reviewController = new ReviewController(deleteHelper, this);

        reviewPagerAdapter = new ReviewPagerAdapter(getSupportFragmentManager());
        binding.viewPagerReview.setAdapter(reviewPagerAdapter);
        binding.pagerIndicatorReview.setViewPager(binding.viewPagerReview);
        binding.pbReviewImage.setVisibility(View.VISIBLE);

        Drawable d[]=binding.skipImage.getCompoundDrawablesRelative();
        d[2].setColorFilter(getApplicationContext().getResources().getColor(R.color.button_blue), PorterDuff.Mode.SRC_IN);

        /**
         * Restores the previous state of the activity or initializes a new review session.
         * Checks if there's a saved media state from a previous session (e.g., before screen rotation).
         * If found, restores the last viewed image and its detail view.
         * Otherwise, starts a new random image review session.
         *
         * @param savedInstanceState Bundle containing the activity's previously saved state, if any
         */
        if (savedInstanceState != null && savedInstanceState.getParcelable(SAVED_MEDIA) != null) {
            // Restore the previously viewed image if state exists
            updateImage(savedInstanceState.getParcelable(SAVED_MEDIA));
            // Restore media detail view (handles configuration changes like screen rotation)
            setUpMediaDetailOnOrientation();
        } else {
            // Start fresh review session with a random image
            runRandomizer();
        }

        binding.skipImage.setOnClickListener(view -> {
            reviewImageFragment = getInstanceOfReviewImageFragment();
            reviewImageFragment.disableButtons();
            runRandomizer();
        });

        binding.reviewImageView.setOnClickListener(view ->setUpMediaDetailFragment());

        binding.skipImage.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP && event.getRawX() >= (
                    binding.skipImage.getRight() - binding.skipImage
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

    /**
     * Initiates the process of loading a random media file for review.
     * This method:
     * - Resets the UI state
     * - Shows loading indicator
     * - Manages media cache
     * - Either loads from cache or fetches new media
     *
     * The method is annotated with @SuppressLint("CheckResult") as the Observable
     * subscription is handled through CompositeDisposable in the implementation.
     *
     * @return true indicating successful initiation of the randomization process
     */
    @SuppressLint("CheckResult")
    public boolean runRandomizer() {
        // Reset flag for tracking presence of non-hidden categories
        hasNonHiddenCategories = false;
        // Display loading indicator while fetching media
        binding.pbReviewImage.setVisibility(View.VISIBLE);
        // Reset view pager to first page
        binding.viewPagerReview.setCurrentItem(0);

        // Check cache status and determine source of next media
        if (cachedMedia.isEmpty() || isCacheExpired()) {
            // Fetch and cache new media if cache is empty or expired
            fetchAndCacheMedia();
        } else {
            // Use next media file from existing cache
            processNextCachedMedia();
        }
        return true;
    }

    /**
     * Batch checks whether multiple files from the cache are used in wikis.
     * This is a more efficient way to process multiple files compared to checking them one by one.
     *
     * @param mediaList List of Media objects to check for usage
     */
    /**
     * Batch checks whether multiple files from the cache are used in wikis.
     * This is a more efficient way to process multiple files compared to checking them one by one.
     *
     * @param mediaList List of Media objects to check for usage
     */
    private void batchCheckFilesUsage(List<Media> mediaList) {
        // Extract filenames from media objects
        List<String> filenames = new ArrayList<>();
        for (Media media : mediaList) {
            if (media.getFilename() != null) {
                filenames.add(media.getFilename());
            }
        }

        compositeDisposable.add(
            reviewHelper.checkFileUsageBatch(filenames)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribe(results -> {
                    // Process each result
                    for (kotlin.Pair<String, Boolean> result : results) {
                        String filename = result.getFirst();
                        Boolean isUsed = result.getSecond();

                        // Find corresponding media object
                        for (Media media : mediaList) {
                            if (filename.equals(media.getFilename())) {
                                if (!isUsed) {
                                    // If file is not used, proceed with category check
                                    findNonHiddenCategories(media);
                                }
                                break;
                            }
                        }
                    }
                }, this::handleError));
    }


    /**
     * Fetches and caches new media files for review.
     * Uses RxJava to:
     * - Generate a range of indices for the desired cache size
     * - Fetch random media files asynchronously
     * - Handle thread scheduling between IO and UI operations
     * - Store the fetched media in cache
     *
     * The operation is added to compositeDisposable for proper lifecycle management.
     */
    private void fetchAndCacheMedia() {
        compositeDisposable.add(
            Observable.range(0, CACHE_SIZE)
                .flatMap(i -> reviewHelper.getRandomMedia().toObservable())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribe(mediaList -> {
                    // Clear existing cache
                    cachedMedia.clear();

                    // Start batch check process
                    batchCheckFilesUsage(mediaList);

                    // Update cache with new media
                    cachedMedia.addAll(mediaList);
                    updateLastCacheTime();

                    // Process first media item if available
                    if (!cachedMedia.isEmpty()) {
                        processNextCachedMedia();
                    }
                }, this::handleError));
    }

    /**
     * Processes the next media file from the cache.
     * If cache is not empty, removes and processes the first media file.
     * If cache is empty, triggers a new fetch operation.
     *
     * This method ensures continuous flow of media files for review
     * while maintaining the cache mechanism.
     */
    private void processNextCachedMedia() {
        if (!cachedMedia.isEmpty()) {
            // Remove and get the first media from cache
            Media media = cachedMedia.remove(0);

            checkWhetherFileIsUsedInWikis(media);
        } else {
            // Refill cache if empty
            fetchAndCacheMedia();
        }
    }

    /**
     * Checks if the current cache has expired.
     * Cache expiration is determined by comparing the last cache time
     * with the current time against the configured expiry duration.
     *
     * @return true if cache has expired, false otherwise
     */
    private boolean isCacheExpired() {
        // Get shared preferences instance
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        long lastCacheTime = prefs.getLong(LAST_CACHE_TIME_KEY, 0);

        long currentTime = System.currentTimeMillis();

        return (currentTime - lastCacheTime) > CACHE_EXPIRY_TIME;
    }


    /**
     * Updates the timestamp of the last cache operation.
     * Stores the current time in SharedPreferences to track
     * cache freshness for future operations.
     */
    private void updateLastCacheTime() {

        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        // Store current timestamp as last cache time
        editor.putLong(LAST_CACHE_TIME_KEY, System.currentTimeMillis());
        // Apply changes asynchronously
        editor.apply();
    }

    /**
     * Check whether media is used or not in any Wiki Page
     */
    private void checkWhetherFileIsUsedInWikis(final Media media) {
        compositeDisposable.add(reviewHelper.checkFileUsage(media.getFilename())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(result -> {
                if (!result) {
                    findNonHiddenCategories(media);
                } else {
                    processNextCachedMedia();
                }
            }, this::handleError));
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

    private void updateImage(Media media) {
        reviewHelper.addViewedImagesToDB(media.getPageId());
        this.media = media;
        String fileName = media.getFilename();
        if (fileName.length() == 0) {
            ViewUtil.showShortSnackbar(binding.drawerLayout, R.string.error_review);
            return;
        }

        if (media.getUser() != null && media.getUser().equals(AccountUtil.getUserName(getApplicationContext()))) {
            processNextCachedMedia();
            return;
        }

        binding.reviewImageView.setImageURI(media.getImageUrl());

        reviewController.onImageRefreshed(media);
        compositeDisposable.add(reviewHelper.getFirstRevisionOfFile(fileName)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(revision -> {
                reviewController.firstRevision = revision;
                reviewPagerAdapter.updateFileInformation();
                String caption = String.format(getString(R.string.review_is_uploaded_by), fileName, revision.getUser());
                binding.tvImageCaption.setText(caption);
                binding.pbReviewImage.setVisibility(View.GONE);
                reviewImageFragment = getInstanceOfReviewImageFragment();
                reviewImageFragment.enableButtons();
            }, this::handleError));
        binding.viewPagerReview.setCurrentItem(0);
    }

    public void swipeToNext() {
        int nextPos = binding.viewPagerReview.getCurrentItem() + 1;
        // If currently at category fragment, then check whether the media has any non-hidden category
        if (nextPos <= 3) {
            binding.viewPagerReview.setCurrentItem(nextPos);
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
        binding = null;
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
    /**
     * Handles errors that occur during media processing operations.
     * This is a generic error handler that:
     * - Hides the loading indicator
     * - Shows a user-friendly error message via Snackbar
     *
     * Used as error callback for RxJava operations and other async tasks.
     *
     * @param error The Throwable that was caught during operation
     */
    private void handleError(Throwable error) {
        binding.pbReviewImage.setVisibility(View.GONE);
        // Show error message to user via Snackbar
        ViewUtil.showShortSnackbar(binding.drawerLayout, R.string.error_review);
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
        int currentItemOfReviewPager = binding.viewPagerReview.getCurrentItem();
        reviewImageFragment = (ReviewImageFragment) reviewPagerAdapter.instantiateItem(binding.viewPagerReview, currentItemOfReviewPager);
        return reviewImageFragment;
    }

    /**
     * set up the media detail fragment when click on the review image
     */
    private void setUpMediaDetailFragment() {
        if (binding.mediaDetailContainer.getVisibility() == View.GONE && media != null) {
            binding.mediaDetailContainer.setVisibility(View.VISIBLE);
            binding.reviewActivityContainer.setVisibility(View.INVISIBLE);
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
        if (binding.mediaDetailContainer.getVisibility() == View.VISIBLE) {
            binding.mediaDetailContainer.setVisibility(View.GONE);
            binding.reviewActivityContainer.setVisibility(View.VISIBLE);
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
            binding.mediaDetailContainer.setVisibility(View.VISIBLE);
            binding.reviewActivityContainer.setVisibility(View.INVISIBLE);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.mediaDetailContainer, mediaDetailFragment).commit();
        }
    }

}
