package fr.free.nrw.commons.explore

import android.content.Context
import android.os.Looper.getMainLooper
import android.view.*
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.tabs.TabLayout
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.databinding.FragmentExploreBinding
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ExploreFragmentUnitTest {

    private lateinit var fragment: ExploreFragment
    private lateinit var fragmentManager: FragmentManager
    private lateinit var context: Context
    private lateinit var activity: MainActivity
    private lateinit var binding : FragmentExploreBinding


    @Mock
    private lateinit var exploreRootFragment: ExploreListRootFragment

    @Mock
    private lateinit var inflater: MenuInflater

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()

        OkHttpConnectionFactory.CLIENT = createTestClient()

        activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        fragment = ExploreFragment()
        fragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        binding = FragmentExploreBinding.inflate(LayoutInflater.from(context))

    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testSetScrollCaseTrue() {
        fragment.setScroll(true)
        Assert.assertEquals(binding.viewPager.isCanScroll, true)
    }

    @Test
    @Throws(Exception::class)
    fun testSetScrollCaseFalse() {
        fragment.setScroll(false)
        Assert.assertEquals(binding.viewPager.isCanScroll, false)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressedCaseTrueSelectedTabZero() {
        Whitebox.setInternalState(fragment, "featuredRootFragment", exploreRootFragment)
        `when`(binding.tabLayout.selectedTabPosition).thenReturn(0)
        `when`(exploreRootFragment.backPressed()).thenReturn(true)
        Assert.assertEquals(fragment.onBackPressed(), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressedCaseTrueSelectedTabNonZero() {
        Whitebox.setInternalState(fragment, "mobileRootFragment", exploreRootFragment)
        `when`(binding.tabLayout.selectedTabPosition).thenReturn(1)
        `when`(exploreRootFragment.backPressed()).thenReturn(true)
        Assert.assertEquals(fragment.onBackPressed(), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressedCaseFalseSelectedTabZero() {
        Whitebox.setInternalState(fragment, "featuredRootFragment", exploreRootFragment)
        `when`(binding.tabLayout.selectedTabPosition).thenReturn(0)
        `when`(exploreRootFragment.backPressed()).thenReturn(false)
        Assert.assertEquals(fragment.onBackPressed(), false)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressedCaseFalseSelectedTabNonZero() {
        Whitebox.setInternalState(fragment, "mobileRootFragment", exploreRootFragment)
        `when`(binding.tabLayout.selectedTabPosition).thenReturn(1)
        `when`(exploreRootFragment.backPressed()).thenReturn(false)
        Assert.assertEquals(fragment.onBackPressed(), false)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseTrue() {
        val menuItem: MenuItem = RoboMenuItem(R.id.action_search)
        Assert.assertEquals(fragment.onOptionsItemSelected(menuItem), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseDefault() {
        val menuItem: MenuItem = RoboMenuItem(android.R.id.home)
        Assert.assertEquals(fragment.onOptionsItemSelected(menuItem), false)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateOptionsMenu() {
        Shadows.shadowOf(getMainLooper()).idle()
        val menu: Menu = RoboMenu(context)
        fragment.onCreateOptionsMenu(menu, inflater)
        verify(inflater).inflate(R.menu.menu_search, menu)
    }

}
