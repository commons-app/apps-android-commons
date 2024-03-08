package fr.free.nrw.commons.category

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class GridViewAdapterUnitTest {

    private lateinit var gridViewAdapter: GridViewAdapter
    private lateinit var activity: CategoryDetailsActivity
    private lateinit var context: Context
    private lateinit var convertView: View

    @Mock
    private lateinit var media1: Media

    @Mock
    private lateinit var parent: ViewGroup

    @Mock
    private lateinit var images: List<Media>

    @Mock
    private lateinit var textView: TextView

    @Before
    @Throws(Exception::class)
    fun setUp() {

        MockitoAnnotations.openMocks(this)


        context = ApplicationProvider.getApplicationContext()

        SoLoader.setInTestMode()

        Fresco.initialize(context)

        activity = Robolectric.buildActivity(CategoryDetailsActivity::class.java).get()

        convertView = LayoutInflater.from(activity)
            .inflate(R.layout.layout_category_images, null) as View

        gridViewAdapter = GridViewAdapter(context, 0, images)
    }

    @Test
    fun testAddItems() {
        gridViewAdapter.addItems(images)
    }

    @Test
    fun testContainsAllImageNull() {
        assertThat(gridViewAdapter.containsAll(null), equalTo( false))
    }

    @Test
    fun testContainsAllDataNull() {
        gridViewAdapter = GridViewAdapter(context, 0, null)
        assertThat(gridViewAdapter.containsAll(images), equalTo( false))
    }

    @Test
    fun testContainsAllDataEmpty() {
        gridViewAdapter = GridViewAdapter(context, 0, listOf())
        assertThat(gridViewAdapter.containsAll(images), equalTo( false))
    }

    @Test
    fun testContainsAll() {
        gridViewAdapter = GridViewAdapter(context, 0, listOf(media1))
        `when`(media1.filename).thenReturn("")
        assertThat(gridViewAdapter.containsAll(listOf(media1)), equalTo( true))
    }

    @Test
    fun testGetItem() {
        gridViewAdapter = GridViewAdapter(context, 0, listOf(media1))
        assertThat(gridViewAdapter.getItem(0), equalTo( media1))
    }

    @Test
    fun testIsEmpty() {
        gridViewAdapter = GridViewAdapter(context, 0, null)
        assertThat(gridViewAdapter.isEmpty, equalTo( true))
    }

    @Test
    fun testGetView() {
        gridViewAdapter = GridViewAdapter(context, 0, listOf(media1))
        `when`(media1.mostRelevantCaption).thenReturn("")
        assertThat(gridViewAdapter.getView(0, convertView, parent), equalTo( convertView))
    }

    @Test
    fun testSetUploaderView() {
        `when`(media1.author).thenReturn("author")
        val method: Method = GridViewAdapter::class.java.getDeclaredMethod(
            "setUploaderView", Media::class.java, TextView::class.java
        )
        method.isAccessible = true
        method.invoke(gridViewAdapter, media1, textView)
    }

}