package fr.free.nrw.commons.quiz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class QuizActivityUnitTest {

    private val SAMPLE_ALERT_TITLE_VALUE = "Title"
    private val SAMPLE_ALERT_MESSAGE_VALUE = "Message"

    private lateinit var activity: QuizActivity
    private lateinit var positiveAnswer: Button
    private lateinit var negativeAnswer: Button
    private lateinit var view: View
    private lateinit var context: Context

    @Mock
    private lateinit var quizController: QuizController

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        SoLoader.setInTestMode()
        Fresco.initialize(ApplicationProvider.getApplicationContext())
        activity = Robolectric.buildActivity(QuizActivity::class.java).create().get()
        context = mock(Context::class.java)
        view = LayoutInflater.from(activity)
            .inflate(R.layout.answer_layout, null) as View
        Mockito.`when`(context.getString(Mockito.any(Int::class.java)))
            .thenReturn("")
        quizController = QuizController()
        quizController.initialize(context)
        positiveAnswer = view.findViewById(R.id.quiz_positive_answer)
        negativeAnswer = view.findViewById(R.id.quiz_negative_answer)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
        Assert.assertNotNull(positiveAnswer)
        Assert.assertNotNull(negativeAnswer)
    }

    @Test
    @Throws(Exception::class)
    fun testSetNextQuestionCaseDefault() {
        activity.setNextQuestion()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testEvaluateScore() {
        Whitebox.setInternalState(activity, "quiz", quizController.getQuiz())
        Whitebox.setInternalState(activity, "questionIndex", 0)
        activity.evaluateScore()
    }

    @Test
    @Throws(Exception::class)
    fun testCustomAlert() {
        activity.customAlert(SAMPLE_ALERT_TITLE_VALUE, SAMPLE_ALERT_MESSAGE_VALUE)
    }

}