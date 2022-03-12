package fr.free.nrw.commons.feedback

import android.content.Context
import fr.free.nrw.commons.feedback.model.Feedback
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class FeedbackContentCreatorUnitTests {
    private lateinit var creator: FeedbackContentCreator
    private lateinit var feedback: Feedback
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.application.applicationContext
    }

    @Test
    fun testToString() {
        feedback = Feedback("123", "apiLevel", "title", "androidVersion", "deviceModel", "mfg", "deviceName", "wifi")
        creator = FeedbackContentCreator(context, feedback)
        Assert.assertNotNull(creator.toString())
    }

}