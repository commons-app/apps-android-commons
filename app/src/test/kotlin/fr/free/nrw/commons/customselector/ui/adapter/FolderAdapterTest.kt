package fr.free.nrw.commons.customselector.ui.adapter

import android.content.ContentResolver
import fr.free.nrw.commons.R
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.TestUtility.setFinalStatic
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Custom Selector Folder Adapter Test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class FolderAdapterTest {

    private var uri: Uri = Mockito.mock(Uri::class.java)
    private lateinit var activity: CustomSelectorActivity
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var image: Image
    private lateinit var folder: Folder
    private lateinit var folderList: ArrayList<Folder>

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        activity = Robolectric.buildActivity(CustomSelectorActivity::class.java).get()
        image = Image(1, "image", uri, "abc/abc", 1, "bucket1")
        folder = Folder(1, "bucket1", ArrayList(listOf(image)))
        folderList = ArrayList(listOf(folder, folder, folder))
        folderAdapter = FolderAdapter(activity, activity as FolderClickListener)
    }

    /**
     * Test on create view holder.
     */
    @Test
    fun onCreateViewHolder() {
        folderAdapter.createViewHolder(GridLayout(activity), 0)
    }

    /**
     * Test on bind view holder.
     */
    @Test
    fun onBindViewHolder() {
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val listItemView: View = inflater.inflate(R.layout.item_custom_selector_folder, null, false)

        whenever(context.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContentResolver.getType(any())).thenReturn("jpg")
        folderAdapter.init(folderList)
        folderAdapter.onBindViewHolder(FolderAdapter.FolderViewHolder(listItemView), 0)
    }

    /**
     * Test init.
     */
    @Test
    fun init() {
        folderAdapter.init(folderList)
    }

    /**
     * Test get item count.
     */
    @Test
    fun getItemCount() {
        assertEquals(0, folderAdapter.itemCount)
    }
}