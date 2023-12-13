package fr.free.nrw.commons.navtab

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.CommonsApplication
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.feedback.model.Feedback
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.profile.ProfileActivity
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowAlertDialog
import org.robolectric.shadows.ShadowDialog
import org.wikipedia.AppAdapter
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MoreBottomSheetFragmentUnitTests {

    private lateinit var fragment: MoreBottomSheetFragment
    private lateinit var view: View
    private lateinit var layoutInflater: LayoutInflater
    private lateinit var context: Context
    private lateinit var activity: ProfileActivity

    @Mock
    private lateinit var store: JsonKvStore

    @Mock
    private lateinit var pageEditClient: PageEditClient

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        AppAdapter.set(TestAppAdapter())

        activity = Robolectric.buildActivity(ProfileActivity::class.java).create().get()
        fragment = MoreBottomSheetFragment()
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(fragment, null)
        fragmentTransaction.commitNowAllowingStateLoss()

        Whitebox.setInternalState(fragment, "store", store)
        Whitebox.setInternalState(fragment, "pageEditClient", pageEditClient)

        `when`(store.getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED)).thenReturn(
            true
        )

        layoutInflater = LayoutInflater.from(activity)
        view = fragment.onCreateView(layoutInflater, null, null) as View
    }

    @Test
    @Throws(Exception::class)
    fun checkFragmentNotNull() {
        Assert.assertNotNull(fragment)
    }

    @Test
    @Throws(Exception::class)
    fun testOnAttach() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onAttach(context)
    }

    @Test
    @Throws(Exception::class)
    fun testOnLogoutClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onLogoutClicked()
        val dialog: AlertDialog = ShadowAlertDialog.getLatestDialog() as AlertDialog
        Assert.assertEquals(dialog.isShowing, true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnFeedbackClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onFeedbackClicked()
        ShadowDialog.getLatestDialog().findViewById<Button>(R.id.btn_submit_feedback).performClick()
    }

    @Test
    @Throws(Exception::class)
    fun testUploadFeedback() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val feedback = mock(Feedback::class.java)
        val observable: Observable<Boolean> = Observable.just(false)
        val observable2: Observable<Boolean> = Observable.just(true)
        doReturn(observable, observable2).`when`(pageEditClient).prependEdit(anyString(), anyString(), anyString())
        fragment.uploadFeedback(feedback)
    }

    @Test
    @Throws(Exception::class)
    fun testOnAboutClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onAboutClicked()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val intentForResult: ShadowActivity.IntentForResult =
            shadowActivity.nextStartedActivityForResult
        val nextActivity: ComponentName? = intentForResult.intent.component
        Assert.assertEquals(nextActivity?.className?.contains(".AboutActivity"), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnTutorialClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onTutorialClicked()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val intentForResult: ShadowActivity.IntentForResult =
            shadowActivity.nextStartedActivityForResult
        val nextActivity: ComponentName? = intentForResult.intent.component
        Assert.assertEquals(nextActivity?.className?.contains(".WelcomeActivity"), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSettingsClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onSettingsClicked()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val intentForResult: ShadowActivity.IntentForResult =
            shadowActivity.nextStartedActivityForResult
        val nextActivity: ComponentName? = intentForResult.intent.component
        Assert.assertEquals(nextActivity?.className?.contains(".SettingsActivity"), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnProfileClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onProfileClicked()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val intentForResult: ShadowActivity.IntentForResult =
            shadowActivity.nextStartedActivityForResult
        val nextActivity: ComponentName? = intentForResult.intent.component
        Assert.assertEquals(nextActivity?.className?.contains(".ProfileActivity"), true)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPeerReviewClicked() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        fragment.onPeerReviewClicked()
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val intentForResult: ShadowActivity.IntentForResult =
            shadowActivity.nextStartedActivityForResult
        val nextActivity: ComponentName? = intentForResult.intent.component
        Assert.assertEquals(nextActivity?.className?.contains(".ReviewActivity"), true)
    }

    @Test
    @Throws(Exception::class)
    fun testSendFeedback() {
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        val method: Method = MoreBottomSheetFragment::class.java.getDeclaredMethod("sendFeedback")
        method.isAccessible = true
        method.invoke(fragment)
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        Assert.assertEquals(startedIntent.action, Intent.ACTION_SENDTO)
        Assert.assertEquals(startedIntent.type, null)
        Assert.assertEquals(startedIntent.`data`, Uri.parse("mailto:"))
        Assert.assertEquals(
            startedIntent.extras?.get(Intent.EXTRA_SUBJECT),
            CommonsApplication.FEEDBACK_EMAIL_SUBJECT
        )
    }

}