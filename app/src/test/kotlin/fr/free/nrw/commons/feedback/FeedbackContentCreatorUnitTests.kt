package fr.free.nrw.commons.feedback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.FakeContextWrapper
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.feedback.model.Feedback
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wikipedia.AppAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class FeedbackContentCreatorUnitTests {
    private lateinit var creator: FeedbackContentCreator
    private lateinit var feedback: Feedback

    private lateinit var context: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        AppAdapter.set(TestAppAdapter())
        context = FakeContextWrapper(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testToString() {
        feedback = Feedback("123", "apiLevel", "title", "androidVersion", "deviceModel", "mfg", "deviceName", "wifi")
        creator = FeedbackContentCreator(context, feedback)
        Assert.assertNotNull(creator.toString())
    }

}