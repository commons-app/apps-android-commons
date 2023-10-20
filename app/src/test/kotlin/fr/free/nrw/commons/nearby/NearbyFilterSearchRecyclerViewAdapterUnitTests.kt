package fr.free.nrw.commons.nearby

import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyFilterSearchRecyclerViewAdapterUnitTests {

    private lateinit var context: Context
    private lateinit var adapter: NearbyFilterSearchRecyclerViewAdapter

    @Mock
    private lateinit var recyclerView: RecyclerView

    @Mock
    private lateinit var callback: NearbyFilterSearchRecyclerViewAdapter.Callback

    @Mock
    private lateinit var viewHolder: NearbyFilterSearchRecyclerViewAdapter.RecyclerViewHolder

    @Mock
    private lateinit var imageView: ImageView

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var linearLayout: LinearLayout

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        adapter = NearbyFilterSearchRecyclerViewAdapter(context, ArrayList<Label>(Label.valuesAsList()), recyclerView)
        viewHolder.placeTypeIcon = imageView
        viewHolder.placeTypeLabel = textView
        viewHolder.placeTypeLayout = linearLayout
    }

    @Test
    @Throws(Exception::class)
    fun checkAdapterNotNull() {
        Assert.assertNotNull(adapter)
    }

    @Test
    @Throws(Exception::class)
    fun testSetCallback() {
        adapter.setCallback(callback)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBindViewHolder() {
        adapter.onBindViewHolder(viewHolder, 0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetItemId() {
        adapter.getItemId(0)
    }

    @Test
    @Throws(Exception::class)
    fun testGetItemCount() {
        Assert.assertEquals(adapter.itemCount, 26)
    }

    @Test
    @Throws(Exception::class)
    fun testGetFilter() {
        adapter.filter
    }

    @Test
    @Throws(Exception::class)
    fun testSetRecyclerViewAdapterItemsGreyedOut() {
        adapter.setRecyclerViewAdapterItemsGreyedOut()
    }

    @Test
    @Throws(Exception::class)
    fun testSetRecyclerViewAdapterAllSelected() {
        adapter.setRecyclerViewAdapterAllSelected()
    }

}