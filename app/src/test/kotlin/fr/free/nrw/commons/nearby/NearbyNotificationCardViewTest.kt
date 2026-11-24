package fr.free.nrw.commons.nearby

import android.view.View
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class NearbyNotificationCardViewTest {
    private lateinit var activity: ProfileActivity
    private lateinit var cardView: NearbyNotificationCardView
    private lateinit var testPlace: Place

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        cardView = NearbyNotificationCardView(activity)
        testPlace =
            Place().apply {
                name = "Test Place"
                distance = "100m"
            }
    }

    /** Updating content when hosted outside MainActivity forces card GONE and avoids crash. */
    @Test
    fun testUpdateContentOutsideMainActivityHidesCard() {
        cardView.updateContent(testPlace)
        assertEquals(View.GONE, cardView.visibility)
    }

    /** Clicking the card when hosted outside MainActivity should safely no-op without crashing. */
    @Test
    fun testClickOutsideMainActivityDoesNotCrash() {
        cardView.setActionListeners(testPlace)
        val clickHandled = cardView.performClick()
        assertTrue(clickHandled)
    }

    /** Swiping the card when hosted outside MainActivity should safely no-op without crashing. */
    @Test
    fun testSwipeOutsideMainActivityDoesNotCrash() {
        val swipeHandled = cardView.onSwipe(cardView)
        assertTrue(swipeHandled)
    }
}
