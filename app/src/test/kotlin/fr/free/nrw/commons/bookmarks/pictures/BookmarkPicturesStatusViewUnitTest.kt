package fr.free.nrw.commons.bookmarks.pictures

import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ProgressBar
import android.widget.TextView
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.databinding.FragmentBookmarksPicturesBinding
import fr.free.nrw.commons.profile.ProfileActivity
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

/**
 * Tests that [BookmarkPicturesStatusView] shows the right message and progress bar state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class BookmarkPicturesStatusViewUnitTest {
    private lateinit var binding: FragmentBookmarksPicturesBinding
    private lateinit var statusView: BookmarkPicturesStatusView

    @Mock
    private lateinit var statusTextView: TextView

    @Mock
    private lateinit var progressBar: ProgressBar

    /**
     * Inflates the real binding and replaces the progress bar and status message with mocks.
     */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        OkHttpConnectionFactory.CLIENT = createTestClient()
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        binding = FragmentBookmarksPicturesBinding.inflate(LayoutInflater.from(activity))
        Whitebox.setInternalState(binding, "statusMessage", statusTextView)
        Whitebox.setInternalState(binding, "loadingImagesProgressBar", progressBar)
        statusView = BookmarkPicturesStatusView(binding)
    }

    /**
     * With no pictures shown, the no internet message must be displayed.
     */
    @Test
    fun testShowNoInternetWhenGridIsEmpty() {
        statusView.showNoInternet(true)
        verify(progressBar).setVisibility(GONE)
        verify(statusTextView).setVisibility(VISIBLE)
        verify(statusTextView).setText(binding.root.context.getString(R.string.no_internet))
    }

    /**
     * With no pictures shown, the "no images found" message must be displayed.
     */
    @Test
    fun testShowErrorViewWhenGridIsEmpty() {
        statusView.showErrorView(true)
        verify(progressBar).setVisibility(GONE)
        verify(statusTextView).setVisibility(VISIBLE)
        verify(statusTextView).setText(binding.root.context.getString(R.string.no_images_found))
    }

    /**
     * With pictures already shown, the status message must stay hidden.
     */
    @Test
    fun testShowErrorViewWhenGridHasPictures() {
        statusView.showErrorView(false)
        verify(progressBar).setVisibility(GONE)
        verify(statusTextView).setVisibility(GONE)
        verify(statusTextView, never()).setVisibility(VISIBLE)
    }

    /**
     * With no bookmarks and no pictures, the "no bookmarks" message must be displayed.
     */
    @Test
    fun testShowEmptyBookmarksWhenGridIsEmpty() {
        statusView.showEmptyBookmarks(true)
        verify(progressBar).setVisibility(GONE)
        verify(statusTextView).setVisibility(VISIBLE)
        verify(statusTextView).setText(binding.root.context.getString(R.string.bookmark_empty))
    }

    /**
     * With pictures already shown, the "no bookmarks" message must stay hidden.
     */
    @Test
    fun testShowEmptyBookmarksWhenGridHasPictures() {
        statusView.showEmptyBookmarks(false)
        verify(progressBar).setVisibility(GONE)
        verify(statusTextView).setVisibility(GONE)
        verify(statusTextView, never()).setVisibility(VISIBLE)
    }
}
