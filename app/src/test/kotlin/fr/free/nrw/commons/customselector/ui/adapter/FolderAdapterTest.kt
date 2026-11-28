package fr.free.nrw.commons.customselector.ui.adapter

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image
import fr.free.nrw.commons.customselector.ui.selector.CustomSelectorActivity
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    private fun folderViewHolder(): FolderAdapter.FolderViewHolder {
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return FolderAdapter.FolderViewHolder(inflater.inflate(R.layout.item_custom_selector_folder, null, false))
    }

    /**
     * A folder whose pictures were all handled is greyed out and shows the Commons icon.
     */
    @Test
    fun handledFolderIsGreyedOut() {
        val holder = folderViewHolder()

        holder.setHandled(true)

        assertEquals(View.VISIBLE, holder.handledGroup.visibility)
        assertNotNull(holder.image.colorFilter)
        assertEquals(
            activity.getString(R.string.custom_selector_folder_handled),
            holder.itemView.findViewById<View>(R.id.folder_handled_icon).contentDescription,
        )
    }

    /**
     * A recycled view of a handled folder is shown as usual for a folder that is not handled.
     */
    @Test
    fun folderThatIsNotHandledIsShownAsUsual() {
        val holder = folderViewHolder()
        holder.setHandled(true)

        holder.setHandled(false)

        assertEquals(View.GONE, holder.handledGroup.visibility)
        assertNull(holder.image.colorFilter)
    }

    /**
     * Only the folders that were found to be handled are shown as handled.
     */
    @Test
    fun setHandledFolders() {
        val otherFolder = Folder(2, "bucket2", ArrayList(listOf(image)))
        folderAdapter.init(listOf(folder, otherFolder))

        folderAdapter.setHandledFolders(setOf(2L))
        assertFalse(folderAdapter.isFolderHandled(1))
        assertTrue(folderAdapter.isFolderHandled(2))

        // A picture was added to the folder, so it is not handled anymore
        folderAdapter.setHandledFolders(emptySet())
        assertFalse(folderAdapter.isFolderHandled(2))
    }
}
