package fr.free.nrw.commons.auth

import android.accounts.Account
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestAppAdapter
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.fakes.RoboMenuItem
import org.wikipedia.AppAdapter
import org.wikipedia.login.LoginResult
import java.lang.reflect.Method


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class LoginActivityUnitTests {

    private lateinit var menuItem: MenuItem
    private lateinit var context: Context

    @Mock
    private lateinit var activity: LoginActivity

    @Mock
    private lateinit var progressDialog: ProgressDialog

    @Mock
    private lateinit var view: View

    @Mock
    private lateinit var params: ViewGroup.LayoutParams

    @Mock
    private lateinit var keyEvent: KeyEvent

    @Mock
    private lateinit var textView: TextView

    @Mock
    private lateinit var bundle: Bundle

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var account: Account

    @Mock
    private lateinit var loginResult: LoginResult

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        AppAdapter.set(TestAppAdapter())
        activity = Robolectric.buildActivity(LoginActivity::class.java).create().get()
        context = ApplicationProvider.getApplicationContext()
        menuItem = RoboMenuItem(null)
        Whitebox.setInternalState(activity, "progressDialog", progressDialog)
        Whitebox.setInternalState(activity, "applicationKvStore", applicationKvStore)
        Whitebox.setInternalState(activity, "sessionManager", sessionManager)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnEditorActionCaseDefault() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onEditorAction",
            TextView::class.java,
            Int::class.java,
            KeyEvent::class.java
        )
        method.isAccessible = true
        method.invoke(activity, textView, 0, keyEvent)
    }

    @Test
    @Throws(Exception::class)
    fun testSkipLogin() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "skipLogin"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testForgotPassword() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "forgotPassword"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPrivacyPolicyClicked() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onPrivacyPolicyClicked"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testSignUp() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "signUp"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPostCreate() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onPostCreate",
            Bundle::class.java
        )
        method.isAccessible = true
        method.invoke(activity, bundle)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroy() {
        `when`(progressDialog.isShowing).thenReturn(true)
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onDestroy"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnDestroyWithException() {
        `when`(progressDialog.isShowing).thenThrow(NullPointerException())
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onDestroy"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnLoginSuccessCaseDefault() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onLoginSuccess",
            LoginResult::class.java
        )
        method.isAccessible = true
        method.invoke(activity, loginResult)
    }

    @Test
    @Throws(Exception::class)
    fun testOnLoginSuccess() {
        `when`(progressDialog.isShowing).thenReturn(true)
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onLoginSuccess",
            LoginResult::class.java
        )
        method.isAccessible = true
        method.invoke(activity, loginResult)
    }

    @Test
    @Throws(Exception::class)
    fun testShowPasswordResetPrompt() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "showPasswordResetPrompt"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testHideProgress() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "hideProgress"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnResume() {
        `when`(applicationKvStore.getBoolean("firstrun", true)).thenReturn(true)
        `when`(applicationKvStore.getBoolean("login_skipped", false)).thenReturn(true)
        `when`(sessionManager.currentAccount).thenReturn(account)
        `when`(sessionManager.isUserLoggedIn).thenReturn(true)
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onResume"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnStart() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onStart"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnStop() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onStop"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnPostResume() {
        val method: Method = LoginActivity::class.java.getDeclaredMethod(
            "onPostResume"
        )
        method.isAccessible = true
        method.invoke(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testShowMessageAndCancelDialog() {
        activity.showMessageAndCancelDialog("")
    }

    @Test
    @Throws(Exception::class)
    fun testStartMainActivity() {
        activity.startMainActivity()
    }

    @Test
    @Throws(Exception::class)
    fun testShowMessageAndCancelDialogRes() {
        activity.showMessageAndCancelDialog(R.color.secondaryDarkColor)
    }

    @Test
    @Throws(Exception::class)
    fun testAskUserForTwoFactorAuth() {
        activity.askUserForTwoFactorAuth()
    }

    @Test
    @Throws(Exception::class)
    fun testShowSuccessAndDismissDialog() {
        activity.showSuccessAndDismissDialog()
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelectedCaseDefault() {
        activity.onOptionsItemSelected(menuItem)
    }

    @Test
    @Throws(Exception::class)
    fun testGetMenuInflater() {
        activity.menuInflater
    }

    @Test
    @Throws(Exception::class)
    fun testPerformLogin() {
        activity.performLogin()
    }

    @Test
    @Throws(Exception::class)
    fun testSetContentView() {
        activity.setContentView(view, params)
    }
}
