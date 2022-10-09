package fr.free.nrw.commons

import android.content.Intent
import android.widget.TextView
import fr.free.nrw.commons.quiz.QuizActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowIntent

/**
 * Tests Welcome Activity Methods
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class WelcomeActivityUnitTest {

    private lateinit var activity: WelcomeActivity
    private lateinit var finishTutorialButton: TextView

    /**
     * Setup the Class and Views for Test
     * Initialise the activity with isQuiz as true for Intent Extra
     */
    @Before
    fun setUp() {
        val intent = Intent().putExtra("isQuiz", true)
        activity = Robolectric.buildActivity(WelcomeActivity::class.java, intent)
            .get()
        activity.onCreate(null)
        finishTutorialButton = activity.findViewById(R.id.finishTutorialButton)
    }

    /**
     * Checks if the activity is not null
     */
    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        assertNotNull(activity)
    }

    /**
     * Checks if the activity onDestroy method launches correct intent when isQuiz is true
     */
    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        activity.onDestroy()
        val shadowActivity: ShadowActivity = shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        val shadowIntent: ShadowIntent = shadowOf(startedIntent)
        assertEquals(shadowIntent.intentClass, QuizActivity::class.java)
    }

    /**
     * Checks if the finish Tutorial Button executes the finishTutorial method without any errors
     */
    @Test
    @Throws(Exception::class)
    fun testFinishTutorial() {
        finishTutorialButton.performClick()
    }

    /**
     * Checks if the onBackPressed method executes without any errors
     */
    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

}