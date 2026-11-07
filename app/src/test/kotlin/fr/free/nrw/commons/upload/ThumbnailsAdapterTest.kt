package fr.free.nrw.commons.upload

import android.app.Activity
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.filepicker.UploadableFile
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ThumbnailsAdapterTest {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ThumbnailsAdapter
    private val uploadableFiles =
        mutableListOf(
            UploadableFile(File("first.jpg")),
            UploadableFile(File("second.jpg")),
            UploadableFile(File("third.jpg")),
        )

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        Fresco.initialize(activity)

        adapter = ThumbnailsAdapter { 0 }
        adapter.uploadableFiles = uploadableFiles

        recyclerView = RecyclerView(activity)
        recyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.itemAnimator = null
        recyclerView.adapter = adapter
        activity.setContentView(recyclerView)
        layoutRecyclerView()
    }

    @Test
    fun testDeleteThumbnailAfterEarlierThumbnailRemoved() {
        var deletedPosition = -1
        adapter.onThumbnailDeletedListener =
            ThumbnailsAdapter.OnThumbnailDeletedListener { deletedPosition = it }

        // Remove the first file the same way UploadActivity.onUploadMediaDeleted does
        uploadableFiles.removeAt(0)
        adapter.notifyItemRemoved(0)
        layoutRecyclerView()

        // The last thumbnail was at position 2 before the removal, and is at 1 now
        val viewHolder =
            recyclerView.findViewHolderForAdapterPosition(1) as ThumbnailsAdapter.ViewHolder
        viewHolder.binding.icCross.performClick()

        assertEquals(1, deletedPosition)
    }

    private fun layoutRecyclerView() {
        val spec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        recyclerView.measure(spec, spec)
        recyclerView.layout(0, 0, 1000, 1000)
    }
}
