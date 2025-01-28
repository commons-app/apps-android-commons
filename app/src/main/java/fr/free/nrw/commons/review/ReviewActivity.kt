package fr.free.nrw.commons.review

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.auth.getUserName
import fr.free.nrw.commons.databinding.ActivityReviewBinding
import fr.free.nrw.commons.delete.DeleteHelper
import fr.free.nrw.commons.media.MediaDetailFragment
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.DialogUtil
import fr.free.nrw.commons.utils.ViewUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.Locale
import javax.inject.Inject

class ReviewActivity : BaseActivity() {

    private lateinit var binding: ActivityReviewBinding

    private var mediaDetailFragment: MediaDetailFragment? = null
    lateinit var reviewPagerAdapter: ReviewPagerAdapter
    lateinit var reviewController: ReviewController

    @Inject
    lateinit var reviewHelper: ReviewHelper

    @Inject
    lateinit var deleteHelper: DeleteHelper

    /**
     * Represent fragment for ReviewImage
     * Use to call some methods of ReviewImage fragment
     */
    private var reviewImageFragment: ReviewImageFragment? = null
    private var hasNonHiddenCategories = false
    var media: Media? = null

    private val savedMedia = "saved_media"

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        media?.let {
            outState.putParcelable(savedMedia, it)
        }
    }

    /**
     * Consumers should be simply using this method to use this activity.
     *
     * @param context
     * @param title Page title
     */
    companion object {
        fun startYourself(context: Context, title: String) {
            val reviewActivity = Intent(context, ReviewActivity::class.java)
            reviewActivity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            reviewActivity.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            context.startActivity(reviewActivity)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarBinding?.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        reviewController = ReviewController(deleteHelper, this)

        reviewPagerAdapter = ReviewPagerAdapter(supportFragmentManager)
        binding.viewPagerReview.adapter = reviewPagerAdapter
        binding.pagerIndicatorReview.setViewPager(binding.viewPagerReview)
        binding.pbReviewImage.visibility = View.VISIBLE

        binding.skipImage.compoundDrawablesRelative[2]?.setColorFilter(
            resources.getColor(R.color.button_blue),
            PorterDuff.Mode.SRC_IN
        )

        if (savedInstanceState?.getParcelable<Media>(savedMedia) != null) {
            updateImage(savedInstanceState.getParcelable(savedMedia)!!)
            setUpMediaDetailOnOrientation()
        } else {
            runRandomizer()
        }

        binding.skipImage.setOnClickListener {
            reviewImageFragment = getInstanceOfReviewImageFragment()
            reviewImageFragment?.disableButtons()
            runRandomizer()
        }

        binding.reviewImageView.setOnClickListener {
            setUpMediaDetailFragment()
        }

        binding.skipImage.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= (binding.skipImage.right - binding.skipImage.compoundDrawables[2].bounds.width())
            ) {
                showSkipImageInfo()
                true
            } else {
                false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @SuppressLint("CheckResult")
    fun runRandomizer(): Boolean {
        hasNonHiddenCategories = false
        binding.pbReviewImage.visibility = View.VISIBLE
        binding.viewPagerReview.currentItem = 0

        compositeDisposable.add(
            reviewHelper.getRandomMedia()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::checkWhetherFileIsUsedInWikis)
        )
        return true
    }

    /**
     * Check whether media is used or not in any Wiki Page
     */
    @SuppressLint("CheckResult")
    private fun checkWhetherFileIsUsedInWikis(media: Media) {
        compositeDisposable.add(
            reviewHelper.checkFileUsage(media.filename)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { result ->
                    if (!result) {
                        findNonHiddenCategories(media)
                    } else {
                        runRandomizer()
                    }
                }
        )
    }

    /**
     * Finds non-hidden categories and updates current image
     */
    private fun findNonHiddenCategories(media: Media) {
        this.media = media
        // If non-hidden category is found then set hasNonHiddenCategories to true
        // so that category review cannot be skipped
        hasNonHiddenCategories = media.categoriesHiddenStatus.values.any { !it }
        reviewImageFragment = getInstanceOfReviewImageFragment()
        reviewImageFragment?.disableButtons()
        updateImage(media)
    }

    @SuppressLint("CheckResult")
    private fun updateImage(media: Media) {
        reviewHelper.addViewedImagesToDB(media.pageId)
        this.media = media
        val fileName = media.filename

        if (fileName.isNullOrEmpty()) {
            ViewUtil.showShortSnackbar(binding.drawerLayout, R.string.error_review)
            return
        }

        //If The Media User and Current Session Username is same then Skip the Image
        if (media.user == getUserName(applicationContext)) {
            runRandomizer()
            return
        }

        binding.reviewImageView.setImageURI(media.thumbUrl)

        reviewController.onImageRefreshed(media)    // filename is updated
        compositeDisposable.add(
            reviewHelper.getFirstRevisionOfFile(fileName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { revision ->
                    reviewController.firstRevision = revision
                    reviewPagerAdapter.updateFileInformation()
                    val caption = getString(
                        R.string.review_is_uploaded_by,
                        fileName,
                        revision.user()
                    )
                    binding.tvImageCaption.text = caption
                    binding.pbReviewImage.visibility = View.GONE
                    reviewImageFragment = getInstanceOfReviewImageFragment()
                    reviewImageFragment?.enableButtons()
                }
        )
        binding.viewPagerReview.currentItem = 0
    }

    fun swipeToNext() {
        val nextPos = binding.viewPagerReview.currentItem + 1

        // If currently at category fragment, then check whether the media has any non-hidden category
        if (nextPos <= 3) {
            binding.viewPagerReview.currentItem = nextPos
            if (nextPos == 2 && !hasNonHiddenCategories)
            {
                // The media has no non-hidden category. Such media are already flagged by server-side bots, so no need to review manually.
                swipeToNext()
            }
        } else {
            runRandomizer()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    fun showSkipImageInfo() {
        DialogUtil.showAlertDialog(
            this,
            getString(R.string.skip_image).uppercase(Locale.ROOT),
            getString(R.string.skip_image_explanation),
            getString(android.R.string.ok),
            null,
            null,
            null
        )
    }

    fun showReviewImageInfo() {
        DialogUtil.showAlertDialog(
            this,
            getString(R.string.title_activity_review),
            getString(R.string.review_image_explanation),
            getString(android.R.string.ok),
            null,
            null,
            null
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_review_activty, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_image_info -> {
                showReviewImageInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * this function return the instance of  reviewImageFragment
     */
    private fun getInstanceOfReviewImageFragment(): ReviewImageFragment? {
        val currentItemOfReviewPager = binding.viewPagerReview.currentItem
        return reviewPagerAdapter.instantiateItem(
            binding.viewPagerReview,
            currentItemOfReviewPager
        ) as? ReviewImageFragment
    }

    /**
     * set up the media detail fragment when click on the review image
     */
    private fun setUpMediaDetailFragment() {
        if (binding.mediaDetailContainer.visibility == View.GONE && media != null) {
            binding.mediaDetailContainer.visibility = View.VISIBLE
            binding.reviewActivityContainer.visibility = View.INVISIBLE
            val fragmentManager = supportFragmentManager
            mediaDetailFragment = MediaDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("media", media)
                }
            }
            fragmentManager.beginTransaction()
                .add(R.id.mediaDetailContainer, mediaDetailFragment!!)
                .addToBackStack("MediaDetail")
                .commit()
        }
    }

    /**
     * handle the back pressed event of this activity
     * this function call every time when back button is pressed
     */
    @Deprecated("This method has been deprecated in favor of using the" +
            "{@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}." +
            "The OnBackPressedDispatcher controls how back button events are dispatched" +
            "to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (binding.mediaDetailContainer.visibility == View.VISIBLE) {
            binding.mediaDetailContainer.visibility = View.GONE
            binding.reviewActivityContainer.visibility = View.VISIBLE
        }
        super.onBackPressed()
    }

    /**
     * set up media detail fragment after orientation change
     */
    private fun setUpMediaDetailOnOrientation() {
        val fragment = supportFragmentManager.findFragmentById(R.id.mediaDetailContainer)
        fragment?.let {
            binding.mediaDetailContainer.visibility = View.VISIBLE
            binding.reviewActivityContainer.visibility = View.INVISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.mediaDetailContainer, it)
                .commit()
        }
    }
}

