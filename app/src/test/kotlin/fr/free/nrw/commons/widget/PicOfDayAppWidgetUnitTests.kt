package fr.free.nrw.commons.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wikipedia.AppAdapter
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class PicOfDayAppWidgetUnitTests {

    private lateinit var widget: PicOfDayAppWidget
    private lateinit var context: Context

    @Mock
    private lateinit var views: RemoteViews

    @Mock
    private lateinit var appWidgetManager: AppWidgetManager

    @Mock
    private lateinit var mediaClient: MediaClient

    @Mock
    private lateinit var compositeDisposable: CompositeDisposable

    @Before
    fun setUp() {
        AppAdapter.set(TestAppAdapter())
        context = ApplicationProvider.getApplicationContext()
        SoLoader.setInTestMode()
        ImagePipelineFactory.initialize(context)
        MockitoAnnotations.openMocks(this)
        widget = PicOfDayAppWidget()
        Whitebox.setInternalState(widget, "compositeDisposable", compositeDisposable)
        Whitebox.setInternalState(widget, "mediaClient", mediaClient)
    }

    @Test
    @Throws(Exception::class)
    fun testWidgetNotNull() {
        Assert.assertNotNull(widget)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDisabled() {
        widget.onDisabled(context)
    }

    @Test
    @Throws(Exception::class)
    fun testOnEnabled() {
        widget.onEnabled(context)
    }

    @Test
    @Throws(Exception::class)
    fun testOnUpdate() {
        widget.onUpdate(context, appWidgetManager, intArrayOf(1))
    }

    @Test
    @Throws(Exception::class)
    fun testLoadImageFromUrl() {
        val method: Method = PicOfDayAppWidget::class.java.getDeclaredMethod(
            "loadImageFromUrl",
            String::class.java,
            Context::class.java,
            RemoteViews::class.java,
            AppWidgetManager::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(widget, "", context, views, appWidgetManager, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testLoadPictureOfTheDay() {
        `when`(mediaClient.getPictureOfTheDay()).thenReturn(Single.just(Media()))
        val method: Method = PicOfDayAppWidget::class.java.getDeclaredMethod(
            "loadPictureOfTheDay",
            Context::class.java,
            RemoteViews::class.java,
            AppWidgetManager::class.java,
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(widget, context, views, appWidgetManager, 1)
    }

}