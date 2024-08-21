package fr.free.nrw.commons.feedback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.FakeContextWrapper
import fr.free.nrw.commons.OkHttpConnectionFactory
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.createTestClient
import fr.free.nrw.commons.feedback.model.Feedback
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class FeedbackContentCreatorUnitTests {
    private lateinit var creator: FeedbackContentCreator
    private lateinit var feedback: Feedback

    private lateinit var context: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        OkHttpConnectionFactory.CLIENT = createTestClient()
        context = FakeContextWrapper(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun testToString() {
        feedback = Feedback("123", "apiLevel", "title", "androidVersion", "deviceModel", "mfg", "deviceName", "wifi")
        creator = FeedbackContentCreator(context, feedback)
        Assert.assertNotNull(creator.getSectionText())
        Assert.assertNotNull(creator.getSectionTitle())
    }

}