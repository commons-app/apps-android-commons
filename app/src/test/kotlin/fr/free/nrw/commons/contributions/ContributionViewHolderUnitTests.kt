package fr.free.nrw.commons.contributions

import android.net.Uri
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.TestUtility.setFinalStatic
import fr.free.nrw.commons.databinding.LayoutContributionBinding
import fr.free.nrw.commons.media.MediaClient
import fr.free.nrw.commons.profile.ProfileActivity
import io.reactivex.disposables.CompositeDisposable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
@PrepareForTest(ContributionViewHolder::class)
class ContributionViewHolderUnitTests {

    private lateinit var contributionViewHolder: ContributionViewHolder
    private lateinit var activity: ProfileActivity
    private lateinit var parent: View

    @Mock
    private lateinit var callback: ContributionsListAdapter.Callback

    @Mock
    private lateinit var mediaClient: MediaClient

    @Mock
    private lateinit var uri: Uri

    @Mock
    private lateinit var contribution: Contribution

    @Mock
    private lateinit var compositeDisposable: CompositeDisposable

    @Mock
    private lateinit var media: Media

    private lateinit var bindind : LayoutContributionBinding

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        SoLoader.setInTestMode()
        Fresco.initialize(ApplicationProvider.getApplicationContext())
        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        parent = LayoutInflater.from(activity).inflate(R.layout.layout_contribution, null)
        contributionViewHolder = ContributionViewHolder(parent, callback, mediaClient)

        bindind = LayoutContributionBinding.bind(parent)

        Whitebox.setInternalState(contributionViewHolder, "binding", bindind)

        setFinalStatic(
                ContributionViewHolder::class.java.getDeclaredField("compositeDisposable"),
                compositeDisposable)
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(contributionViewHolder)
    }

    @Test
    @Throws(Exception::class)
    fun testWikipediaButtonClicked() {
        contributionViewHolder.wikipediaButtonClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testImageClicked() {
        contributionViewHolder.imageClicked()
    }

    @Test
    @Throws(Exception::class)
    fun testChooseImageSource() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ContributionViewHolder::class.java.getDeclaredMethod(
            "chooseImageSource",
            String::class.java,
            Uri::class.java
        )
        method.isAccessible = true
        method.invoke(contributionViewHolder, "", uri)
    }

    @Test
    @Throws(Exception::class)
    fun testDisplayWikipediaButton() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = ContributionViewHolder::class.java.getDeclaredMethod(
            "displayWikipediaButton",
            Boolean::class.javaObjectType
        )
        method.isAccessible = true
        method.invoke(contributionViewHolder, false)
    }

    @Test
    @Throws(Exception::class)
    fun testCheckIfMediaExistsOnWikipediaPageCaseNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.wikidataPlace).thenReturn(null)
        val method: Method = ContributionViewHolder::class.java.getDeclaredMethod(
            "checkIfMediaExistsOnWikipediaPage",
            Contribution::class.java
        )
        method.isAccessible = true
        method.invoke(contributionViewHolder, contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseNull() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        contributionViewHolder.init(0, null)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseNonNull_STATE_COMPLETED() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.state).thenReturn(Contribution.STATE_COMPLETED)
        `when`(contribution.media).thenReturn(media)
        `when`(media.mostRelevantCaption).thenReturn("")
        `when`(media.author).thenReturn("")
        contributionViewHolder.init(0, contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseNonNull_STATE_QUEUED() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.state).thenReturn(Contribution.STATE_QUEUED)
        `when`(contribution.media).thenReturn(media)
        `when`(media.mostRelevantCaption).thenReturn("")
        `when`(media.author).thenReturn("")
        contributionViewHolder.init(0, contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseNonNull_STATE_IN_PROGRESS() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.state).thenReturn(Contribution.STATE_IN_PROGRESS)
        `when`(contribution.media).thenReturn(media)
        `when`(media.mostRelevantCaption).thenReturn("")
        `when`(media.author).thenReturn("")
        contributionViewHolder.init(0, contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseNonNull_STATE_PAUSED() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.state).thenReturn(Contribution.STATE_PAUSED)
        `when`(contribution.media).thenReturn(media)
        `when`(media.mostRelevantCaption).thenReturn("")
        `when`(media.author).thenReturn("")
        contributionViewHolder.init(0, contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseNonNull_STATE_FAILED() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.state).thenReturn(Contribution.STATE_FAILED)
        `when`(contribution.media).thenReturn(media)
        `when`(media.mostRelevantCaption).thenReturn("")
        `when`(media.author).thenReturn("")
        contributionViewHolder.init(0, contribution)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseImageSource_HttpURL() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.media).thenReturn(media)
        `when`(contribution.media.thumbUrl).thenReturn("https://demo/sample.png")
        `when`(contribution.localUri).thenReturn(null)
        contributionViewHolder.init(0, contribution)
        Assert.assertNotNull(contributionViewHolder.imageRequest)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseImageSource_NULL() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.media).thenReturn(media)
        `when`(contribution.media.thumbUrl).thenReturn(null)
        `when`(contribution.localUri).thenReturn(null)
        contributionViewHolder.init(0, contribution)
        Assert.assertNull(contributionViewHolder.imageRequest)
    }

    @Test
    @Throws(Exception::class)
    fun testInitCaseImageSource_LocalUri() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        `when`(contribution.media).thenReturn(media)
        `when`(contribution.media.thumbUrl).thenReturn(null)
        `when`(contribution.localUri).thenReturn(Uri.parse("/data/android/demo.png"))
        contributionViewHolder.init(0, contribution)
        Assert.assertNotNull(contributionViewHolder.imageRequest)
    }
}
