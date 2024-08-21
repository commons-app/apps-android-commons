package fr.free.nrw.commons.quiz

import android.app.Activity
import androidx.test.core.app.ApplicationProvider
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.soloader.SoLoader
import com.nhaarman.mockitokotlin2.any
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.auth.SessionManager
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.profile.achievements.FeedbackResponse
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class QuizCheckerUnitTest {

    private lateinit var quizChecker: QuizChecker
    private lateinit var activity: Activity

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Mock
    private lateinit var jsonKvStore: JsonKvStore

    @Mock
    private lateinit var feedbackResponse: FeedbackResponse

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        SoLoader.setInTestMode()
        Fresco.initialize(ApplicationProvider.getApplicationContext())
        activity = Robolectric.buildActivity(QuizActivity::class.java).create().get()
        quizChecker = QuizChecker(sessionManager, okHttpJsonApiClient, jsonKvStore)
        Mockito.`when`(sessionManager.userName).thenReturn("")
        Mockito.`when`(okHttpJsonApiClient.getUploadCount(any())).thenReturn(Single.just(0))
        Mockito.`when`(okHttpJsonApiClient.getAchievements(any()))
            .thenReturn(Single.just(feedbackResponse))
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        Assert.assertNotNull(quizChecker)
    }

    @Test
    @Throws(Exception::class)
    fun testInitQuizCheck() {
        quizChecker.initQuizCheck(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testCleanUp() {
        quizChecker.cleanup()
    }

    @Test
    @Throws(Exception::class)
    fun testSetTotalUploadCount() {
        val method: Method = QuizChecker::class.java.getDeclaredMethod(
            "setTotalUploadCount",
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(quizChecker, -1)
    }

    @Test
    @Throws(Exception::class)
    fun testSetRevertParameter() {
        val method: Method = QuizChecker::class.java.getDeclaredMethod(
            "setRevertParameter",
            Int::class.java
        )
        method.isAccessible = true
        method.invoke(quizChecker, -1)
    }

    @Test
    @Throws(Exception::class)
    fun testCalculateRevertParameterAndShowQuiz() {
        Whitebox.setInternalState(quizChecker, "revertCount", -1)
        val method: Method = QuizChecker::class.java.getDeclaredMethod(
            "calculateRevertParameterAndShowQuiz",
            Activity::class.java
        )
        method.isAccessible = true
        method.invoke(quizChecker, activity)
    }

    @Test
    @Throws(Exception::class)
    fun testCalculateRevertParameterAndShowQuizCaseDefault() {
        Whitebox.setInternalState(quizChecker, "isRevertCountFetched", true)
        Whitebox.setInternalState(quizChecker, "isUploadCountFetched", true)
        Whitebox.setInternalState(quizChecker, "totalUploadCount", 5)
        Whitebox.setInternalState(quizChecker, "revertCount", 5)
        val method: Method = QuizChecker::class.java.getDeclaredMethod(
            "calculateRevertParameterAndShowQuiz",
            Activity::class.java
        )
        method.isAccessible = true
        method.invoke(quizChecker, activity)
    }

    @Test
    @Throws(Exception::class)
    fun testStartQuizActivity() {
        val method: Method = QuizChecker::class.java.getDeclaredMethod(
            "startQuizActivity",
            Activity::class.java
        )
        method.isAccessible = true
        method.invoke(quizChecker, activity)
    }

}