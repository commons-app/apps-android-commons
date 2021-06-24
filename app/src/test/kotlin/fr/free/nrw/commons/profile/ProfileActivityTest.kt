package fr.free.nrw.commons.profile

import android.content.Context
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class ProfileActivityTest {

    private lateinit var activity: ProfileActivity
    private lateinit var profileActivity: ProfileActivity
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        mockContext = PowerMockito.mock(Context::class.java)
        profileActivity = PowerMockito.mock(ProfileActivity::class.java)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        activity.onDestroy()
    }

    @Test
    @Throws(Exception::class)
    fun testStartYourself() {
        ProfileActivity.startYourself(mockContext)
    }

}