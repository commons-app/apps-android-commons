package fr.free.nrw.commons.review

import android.content.Context
import android.os.Looper.getMainLooper
import android.view.Menu
import android.view.MenuItem
import butterknife.BindView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import com.nhaarman.mockitokotlin2.doNothing
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import org.wikipedia.AppAdapter
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
    var reviewPager: ReviewViewPager? = null

    var hasNonHiddenCategories: Boolean = false

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        context = RuntimeEnvironment.application.applicationContext

        AppAdapter.set(TestAppAdapter())

        SoLoader.setInTestMode()

        Fresco.initialize(context)

        activity = Robolectric.buildActivity(ReviewActivity::class.java).create().get()

        menuItem = RoboMenuItem(null)

        menu = RoboMenu(context)
        Whitebox.setInternalState(activity, "reviewPager", reviewPager);
        Whitebox.setInternalState(activity, "hasNonHiddenCategories", hasNonHiddenCategories);

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
        doReturn(1,2).`when`(reviewPager)?.currentItem
        activity.swipeToNext()
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        activity.onDestroy()
    }

    @Test
    @Throws(Exception::class)
    fun testShowSkipImageInfo() {
        activity.showSkipImageInfo()
    }

    @Test
    @Throws(Exception::class)
    fun testShowReviewImageInfo() {
        activity.showReviewImageInfo()
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
        var setUpMediaDetailFragment: Method =
            ReviewActivity::class.java.getDeclaredMethod("setUpMediaDetailFragment")
        setUpMediaDetailFragment.isAccessible = true
        setUpMediaDetailFragment.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSetUpMediaDetailOnOrientation() {
        var setUpMediaDetailFragment: Method =
            ReviewActivity::class.java.getDeclaredMethod("setUpMediaDetailOnOrientation")
        setUpMediaDetailFragment.isAccessible = true
        setUpMediaDetailFragment.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

}