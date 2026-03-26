package fr.free.nrw.commons

import android.content.Intent
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

/**
 * Tests Welcome Activity Methods
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class WelcomeActivityUnitTest {

    private lateinit var activity: WelcomeActivity

    /**
     * Setup the Class and Activity for Test
     * Initialise the activity with isQuiz as true for Intent Extra
     */
    @Before
    fun setUp() {
        val intent = Intent().putExtra("isQuiz", true)
        activity = Robolectric
            .buildActivity(WelcomeActivity::class.java, intent)
            .setup() // setup() automatically calls onCreate, onStart, onResume
            .get()

        // REMOVED: finishTutorialButton = activity.findViewById...
        // (Compose UI cannot be found with XML IDs)
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

        assertNotNull("Intent should not be null", startedIntent)
        assertEquals(QuizActivity::class.java.name, startedIntent.component?.className)
    }

    // REMOVED: testFinishTutorial()
    // UI clicks are now handled exclusively by the Compose Test Rule in the androidTest folder.

    /**
     * Checks if the onBackPressed method executes without any errors
     */
    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressedDispatcher.onBackPressed()
    }
}