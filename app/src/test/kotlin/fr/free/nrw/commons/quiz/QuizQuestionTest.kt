package fr.free.nrw.commons.quiz

import android.net.Uri
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class QuizQuestionTest {
    @Mock
    private lateinit var quizQuestion: QuizQuestion

    private val questionNumSampleValue = 1
    private val questionSampleValue = "Is this picture OK to upload?"
    private val questionUrlSampleValueOne = "https://i.imgur.com/0fMYcpM.jpg"
    private val questionUrlSampleValueTwo = "https://example.com"
    private val isAnswerSampleValue = false
    private val answerMessageSampleValue = "Continue"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        quizQuestion =
            QuizQuestion(
                questionNumSampleValue,
                questionSampleValue,
                questionUrlSampleValueOne,
                isAnswerSampleValue,
                answerMessageSampleValue,
            )
    }

    @Test
    fun testGetUrl() {
        assertEquals(quizQuestion.getUrl(), Uri.parse(questionUrlSampleValueOne))
    }

    @Test
    fun testSetUrl() {
        quizQuestion.setUrl(questionUrlSampleValueTwo)
        assertEquals(quizQuestion.getUrl(), Uri.parse(questionUrlSampleValueTwo))
    }
}
