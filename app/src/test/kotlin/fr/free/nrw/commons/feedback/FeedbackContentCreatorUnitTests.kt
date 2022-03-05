package fr.free.nrw.commons.feedback

import fr.free.nrw.commons.feedback.model.Feedback
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class FeedbackContentCreatorUnitTests {
    private lateinit var creator: FeedbackContentCreator
    private lateinit var feedback: Feedback

    @Before
    fun setup() {

    }

    @Test
    fun testToString() {
        feedback = Feedback("123", "apiLevel", "title", "androidVersion", "deviceModel", "mfg", "deviceName", "wifi")
        creator = FeedbackContentCreator(feedback)
        Assert.assertNotNull(creator.toString())
    }

}