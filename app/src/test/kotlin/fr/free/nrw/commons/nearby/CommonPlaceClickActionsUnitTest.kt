package fr.free.nrw.commons.nearby

import android.net.Uri
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.contributions.ContributionController
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.nearby.fragments.CommonPlaceClickActions
import fr.free.nrw.commons.profile.ProfileActivity
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CommonPlaceClickActionsUnitTest {

    private lateinit var commonPlaceClickActions: CommonPlaceClickActions

    @Mock
    private lateinit var store: JsonKvStore

    @Mock
    private lateinit var contributionController: ContributionController

    @Mock
    private lateinit var place: Place

    @Mock
    private lateinit var uri: Uri

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        commonPlaceClickActions = CommonPlaceClickActions(store, activity, contributionController)
    }

    @Test
    fun testNonNull() {
        Assert.assertNotNull(commonPlaceClickActions)
    }

    @Test
    fun testFunctionDeclaration() {
        Assert.assertNotNull(commonPlaceClickActions.onCameraClicked())
        Assert.assertNotNull(commonPlaceClickActions.onGalleryClicked())
        Assert.assertNotNull(commonPlaceClickActions.onOverflowClicked())
        Assert.assertNotNull(commonPlaceClickActions.onDirectionsClicked())
    }

    @Test
    @Throws(Exception::class)
    fun testStoreSharedPrefs() {
        val method: Method = CommonPlaceClickActions::class.java.getDeclaredMethod(
            "storeSharedPrefs",
            Place::class.java
        )
        method.isAccessible = true
        method.invoke(commonPlaceClickActions, place)
    }

    @Test
    @Throws(Exception::class)
    fun testOpenWebView() {
        val method: Method = CommonPlaceClickActions::class.java.getDeclaredMethod(
            "openWebView",
            Uri::class.java
        )
        method.isAccessible = true
        Assert.assertEquals(method.invoke(commonPlaceClickActions, uri), true)
    }

    @Test
    @Throws(Exception::class)
    fun testShowLoginDialog() {
        val method: Method = CommonPlaceClickActions::class.java.getDeclaredMethod(
            "showLoginDialog"
        )
        method.isAccessible = true
        method.invoke(commonPlaceClickActions)
    }

    @Test
    @Throws(Exception::class)
    fun testSetPositiveButton() {
        val method: Method = CommonPlaceClickActions::class.java.getDeclaredMethod(
            "setPositiveButton"
        )
        method.isAccessible = true
        method.invoke(commonPlaceClickActions)
    }

}