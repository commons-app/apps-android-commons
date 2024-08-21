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

    private val QUESTION_NUM_SAMPLE_VALUE = 1
    private val QUESTION_SAMPLE_VALUE = "Is this picture OK to upload?"
    private val QUESTION_URL_SAMPLE_VALUE_ONE = "https://i.imgur.com/0fMYcpM.jpg"
    private val QUESTION_URL_SAMPLE_VALUE_TWO = "https://example.com"
    private val IS_ANSWER_SAMPLE_VALUE = false
    private val ANSWER_MESSAGE_SAMPLE_VALUE = "Continue"

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        quizQuestion = QuizQuestion(
            QUESTION_NUM_SAMPLE_VALUE,
            QUESTION_SAMPLE_VALUE,
            QUESTION_URL_SAMPLE_VALUE_ONE,
            IS_ANSWER_SAMPLE_VALUE,
            ANSWER_MESSAGE_SAMPLE_VALUE
        )
    }

    @Test
    fun testGetUrl() {
        assertEquals(quizQuestion.getUrl(), Uri.parse(QUESTION_URL_SAMPLE_VALUE_ONE))
    }

    @Test
    fun testSetUrl() {
        quizQuestion.setUrl(QUESTION_URL_SAMPLE_VALUE_TWO)
        assertEquals(quizQuestion.getUrl(), Uri.parse(QUESTION_URL_SAMPLE_VALUE_TWO))
    }

}