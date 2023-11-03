package fr.free.nrw.commons.bookmarks.items

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
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.profile.ProfileActivity
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.wikipedia.AppAdapter
import java.lang.reflect.Method
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class BookmarkItemsFragmentUnitTest {

    private lateinit var fragment: BookmarkItemsFragment
    private lateinit var context: Context
    private lateinit var view: View
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var parentLayout: RelativeLayout

    @Mock
    private lateinit var savedInstanceState: Bundle

    @Mock
    private lateinit var controller: BookmarkItemsController

    /**
     * Get Mock bookmark list.
     */
    private val mockBookmarkList: List<DepictedItem>
        get() {
            val list = ArrayList<DepictedItem>()
            list.add(
                DepictedItem(
                    "name", "description", "image url", listOf("instance"),
                    listOf(
                        CategoryItem("category name", "category description",
                        "category thumbnail", false)
                    ), true, "id")
            )
            return list
        }

    /**
     * fragment Setup
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = BookmarkItemsFragment.newInstance()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commit()

        layoutInflater = LayoutInflater.from(activity)
        view = layoutInflater
            .inflate(R.layout.fragment_bookmarks_items, null) as View

        statusTextView = view.findViewById(R.id.status_message)
        progressBar = view.findViewById(R.id.loading_images_progress_bar)
        recyclerView = view.findViewById(R.id.list_view)

        fragment.statusTextView = statusTextView
        fragment.progressBar = progressBar
        fragment.recyclerView = recyclerView
        fragment.parentLayout = parentLayout
        fragment.controller = controller

    }

    /**
     * test init items when non empty
     */
    @Test
    fun testInitNonEmpty(){
        whenever(controller.loadFavoritesItems()).thenReturn(mockBookmarkList)
        val method: Method =
            BookmarkItemsFragment::class.java.getDeclaredMethod("initList", Context::class.java)
        method.isAccessible = true
        method.invoke(fragment, context)
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
}