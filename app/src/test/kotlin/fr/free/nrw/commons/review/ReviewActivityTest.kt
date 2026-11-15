package fr.free.nrw.commons.review

import android.content.Context
import android.os.Looper.getMainLooper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import com.nhaarman.mockitokotlin2.doNothing
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.databinding.ActivityReviewBinding
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowAlertDialog
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ReviewActivityTest {
    private lateinit var activity: ReviewActivity

    private lateinit var menuItem: MenuItem

    private lateinit var menu: Menu

    private lateinit var context: Context

    @Mock
    private lateinit var reviewPagerAdapter: ReviewPagerAdapter

    @Mock
    var reviewPager: ReviewViewPager? = null

    var hasNonHiddenCategories: Boolean = false

    @Mock
    var reviewHelper: ReviewHelper? = null

    @Mock
    private lateinit var reviewImageFragment: ReviewImageFragment

    private lateinit var binding: ActivityReviewBinding

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        context = ApplicationProvider.getApplicationContext()

        OkHttpConnectionFactory.CLIENT = createTestClient()

        SoLoader.setInTestMode()

        Fresco.initialize(context)

        activity = Robolectric.buildActivity(ReviewActivity::class.java).create().get()
        binding = ActivityReviewBinding.inflate(activity.layoutInflater)

        menuItem = RoboMenuItem(null)

        menu = RoboMenu(context)
        Whitebox.setInternalState(binding, "viewPagerReview", reviewPager)
        Whitebox.setInternalState(activity, "hasNonHiddenCategories", hasNonHiddenCategories)
        Whitebox.setInternalState(activity, "reviewHelper", reviewHelper)
        Whitebox.setInternalState(activity, "reviewImageFragment", reviewImageFragment)
        Whitebox.setInternalState(activity, "reviewPagerAdapter", reviewPagerAdapter)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSupportNavigateUp() {
        activity.onSupportNavigateUp()
    }

    @Test
    @Throws(Exception::class)
    fun testSwipeToNext() {
        shadowOf(getMainLooper()).idle()
        doReturn(1, 2).`when`(reviewPager)?.currentItem
        activity.swipeToNext()
    }

    @Test
    @Throws(Exception::class)
    fun testSwipeToLastFragment() {
        shadowOf(getMainLooper()).idle()
        doReturn(3).`when`(reviewPager)?.currentItem
        val media = mock(Media::class.java)

        doReturn(mapOf<String, Boolean>("test" to false)).`when`(media).categoriesHiddenStatus
        doReturn(Single.just(media)).`when`(reviewHelper)?.getRandomMedia()
        Assert.assertNotNull(reviewHelper?.getRandomMedia())
        reviewHelper
            ?.getRandomMedia()
            ?.test()
            ?.assertValue(media)
        activity.swipeToNext()
    }

    @Test
    @Throws(Exception::class)
    fun testFindNonHiddenCategories() {
        shadowOf(getMainLooper()).idle()
        val media = mock(Media::class.java)
        doReturn(mapOf<String, Boolean>("test" to false)).`when`(media).categoriesHiddenStatus
        doReturn(
            mock(ReviewImageFragment::class.java),
        ).`when`(reviewPagerAdapter).instantiateItem(ArgumentMatchers.any(), anyInt())
        doReturn("").`when`(media).filename
        doNothing().`when`(reviewImageFragment).disableButtons()

        var findNonHiddenCategory: Method =
            ReviewActivity::class.java.getDeclaredMethod("findNonHiddenCategories", Media::class.java)
        findNonHiddenCategory.isAccessible = true
        findNonHiddenCategory.invoke(activity, media)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        Robolectric.buildActivity(ReviewActivity::class.java).create().destroy()
    }

    @Test
    @Throws(Exception::class)
    fun testShowSkipImageInfo() {
        ReviewScreenUi(activity, binding).showSkipImageInfo()
        Assert.assertEquals(
            activity.getString(R.string.skip_image_explanation),
            shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShowReviewImageInfo() {
        ReviewScreenUi(activity, binding).showReviewImageInfo()
        Assert.assertEquals(
            activity.getString(R.string.review_image_explanation),
            shadowOf(ShadowAlertDialog.getLatestAlertDialog()).message,
        )
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateOptionsMenu() {
        activity.onCreateOptionsMenu(menu)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpMediaDetailFragment() {
        binding.mediaDetailContainer.visibility = View.GONE
        ReviewScreenUi(activity, binding).setUpMediaDetailFragment(null)
        Assert.assertEquals(View.GONE, binding.mediaDetailContainer.visibility)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpMediaDetailOnOrientation() {
        ReviewScreenUi(activity, binding).setUpMediaDetailOnOrientation()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

    @Test
    fun testCurrentReviewImageFragmentIsReturned() {
        doReturn(2).`when`(reviewPager)?.currentItem
        doReturn(reviewImageFragment).`when`(reviewPagerAdapter).instantiateItem(binding.viewPagerReview, 2)

        val fragment = ReviewScreenUi(activity, binding).getInstanceOfReviewImageFragment(reviewPagerAdapter)

        Assert.assertSame(reviewImageFragment, fragment)
    }
}
