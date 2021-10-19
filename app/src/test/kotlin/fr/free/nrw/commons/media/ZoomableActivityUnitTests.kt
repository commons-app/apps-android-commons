package fr.free.nrw.commons.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ZoomableActivityUnitTests {

    private lateinit var context: Context
    private lateinit var activity: ZoomableActivity

    @Mock
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        context = RuntimeEnvironment.application.applicationContext
        SoLoader.setInTestMode()
        Fresco.initialize(context)
        val intent = Intent().setData(uri)
        activity = Robolectric.buildActivity(ZoomableActivity::class.java, intent).create().get()
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

}