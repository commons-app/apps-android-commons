package fr.free.nrw.commons.category

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.ViewPagerAdapter
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.explore.categories.media.CategoriesMediaFragment
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CategoryDetailsActivityUnitTests {
    private lateinit var activity: CategoryDetailsActivity

    private lateinit var context: Context

    private lateinit var menuItem: MenuItem

    private lateinit var menu: Menu

    @Mock
    private lateinit var categoriesMediaFragment: CategoriesMediaFragment

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        OkHttpConnectionFactory.CLIENT = createTestClient()

        context = ApplicationProvider.getApplicationContext()

        activity = Robolectric.buildActivity(CategoryDetailsActivity::class.java).create().get()

        val fieldCategoriesMediaFragment: Field =
            CategoryDetailsActivity::class.java.getDeclaredField("categoriesMediaFragment")
        fieldCategoriesMediaFragment.isAccessible = true
        fieldCategoriesMediaFragment.set(activity, categoriesMediaFragment)

        menuItem = RoboMenuItem(null)

        menu = RoboMenu(context)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnMediaClicked() {
        activity.onMediaClicked(0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMediaAtPosition() {
        activity.getMediaAtPosition(0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTotalMediaCount() {
        activity.getTotalMediaCount()
    }

    @Test
    @Throws(Exception::class)
    fun testGetContributionStateAt() {
        activity.getContributionStateAt(0)
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
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testViewPagerNotifyDataSetChanged() {
        activity.viewPagerNotifyDataSetChanged()
    }

    @Test
    fun testCategoryTabsPassCategoryToEveryFragment() {
        activity.intent = Intent().putExtra("categoryName", "Birds")
        val adapter = ViewPagerAdapter(activity, activity.supportFragmentManager)
        val viewModel = mock(CategoryDetailsViewModel::class.java)
        val tabs = CategoryDetailsTabs(activity, adapter, viewModel)

        val mediaFragment = tabs.setTabs("Birds")
        tabs.setPageTitle()

        Assert.assertEquals(3, adapter.count)
        Assert.assertSame(mediaFragment, adapter.getItem(0))
        for (index in 0 until adapter.count) {
            Assert.assertEquals("Birds", adapter.getItem(index).arguments?.getString("categoryName"))
        }
        Assert.assertEquals(activity.getString(R.string.title_for_media).uppercase(), adapter.getPageTitle(0))
        Assert.assertEquals("Birds", activity.title)
        verify(viewModel).onCheckIfBookmarked("Birds")
    }

    @Test
    fun testCategoryTabsWithoutCategoryLeaveArgumentsUnset() {
        val adapter = ViewPagerAdapter(activity, activity.supportFragmentManager)
        val viewModel = mock(CategoryDetailsViewModel::class.java)

        CategoryDetailsTabs(activity, adapter, viewModel).setTabs(null)

        Assert.assertEquals(3, adapter.count)
        for (index in 0 until adapter.count) {
            Assert.assertNull(adapter.getItem(index).arguments)
        }
        verifyNoInteractions(viewModel)
    }
}
