package fr.free.nrw.commons.bookmarks.locations

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.CommonPlaceClickActions
import fr.free.nrw.commons.nearby.fragments.PlaceAdapter
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class BookmarkLocationFragmentUnitTests {

    private lateinit var fragment: BookmarkLocationsFragment
    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var commonPlaceClickActions: CommonPlaceClickActions
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    lateinit var store: JsonKvStore

    @Mock
    private lateinit var parentLayout: RelativeLayout

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Mock
    private lateinit var bookmarkLocationDao: BookmarkLocationsDao

    @Mock
    private lateinit var controller: BookmarkLocationsController

    @Mock
    private lateinit var contributionController: ContributionController

    @Mock
    private lateinit var adapter: PlaceAdapter

    /**
     * Get Mock bookmark list.
     */
    private val mockBookmarkList: List<Place>
        private get() {
            val list = ArrayList<Place>()
            list.add(
                Place(
                    "en",
                    "a place",
                    null,
                    "a description",
                    null,
                    "a cat",
                    null,
                    null,
                    true)
            )
            return list
        }

    /**
     * fragment Setup
     */
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = BookmarkLocationsFragment.newInstance()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        layoutInflater = LayoutInflater.from(activity)
        view = layoutInflater
            .inflate(R.layout.fragment_bookmarks_locations, null) as View

        statusTextView = view.findViewById(R.id.statusMessage)
        progressBar = view.findViewById(R.id.loadingImagesProgressBar)
        recyclerView = view.findViewById(R.id.listView)
        commonPlaceClickActions = CommonPlaceClickActions(store,activity,contributionController)

        fragment.statusTextView = statusTextView
        fragment.progressBar = progressBar
        fragment.recyclerView = recyclerView
        fragment.parentLayout = parentLayout
        fragment.bookmarkLocationDao = bookmarkLocationDao
        fragment.controller = controller
        fragment.commonPlaceClickActions = commonPlaceClickActions
        Whitebox.setInternalState(fragment, "adapter", adapter)

    }

    /**
     * test init places when non empty
     */
    @Test
    fun testInitNonEmpty(){
        whenever(controller.loadFavoritesLocations()).thenReturn(mockBookmarkList)
        val method: Method =
            BookmarkLocationsFragment::class.java.getDeclaredMethod("initList")
        method.isAccessible = true
        method.invoke(fragment)
    }

    /**
     * test onCreateView
     */
    @Test
    @Throws(Exception::class)
    fun testOnCreateView() {
        fragment.onCreateView(layoutInflater,null,savedInstanceState)
    }

    /**
     * check fragment notnull
     */
    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    /**
     * test onViewCreated
     */
    @Test
    @Throws(Exception::class)
    fun testOnViewCreated() {
        fragment.onViewCreated(view, savedInstanceState)
    }

    /**
     * test onResume
     */
    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        fragment.onResume()
    }
}