package fr.free.nrw.commons.auth

import android.accounts.Account
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.auth.login.LoginCallback
import fr.free.nrw.commons.auth.login.LoginClient
import fr.free.nrw.commons.auth.login.LoginResult
import fr.free.nrw.commons.kvstore.JsonKvStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class LoginViewModelTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var loginClient: LoginClient

    @Mock
    private lateinit var sessionManager: SessionManager

    @Mock
    private lateinit var applicationKvStore: JsonKvStore

    private lateinit var viewModel: LoginViewModel

    private val testLoginResult = LoginResult.Result("PASS", "user", "pass", null)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = LoginViewModel(loginClient, sessionManager, applicationKvStore)
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(LoginState.Idle, viewModel.loginState.value)
    }

    @Test
    fun testPerformLoginSuccess() {
        doAnswer { invocation ->
            val callback = invocation.getArgument<LoginCallback>(5)
            callback.success(testLoginResult)
            null
        }.whenever(loginClient).doLogin(eq("user"), eq("pass"), anyOrNull(), eq("1234"), any(), any())

        viewModel.performLogin("user", "pass", "1234")

        verify(sessionManager).setUserLoggedIn(true)
        verify(sessionManager).updateAccount(testLoginResult)
        assertEquals(LoginState.Success(testLoginResult), viewModel.loginState.value)
    }

    @Test
    fun testPerformLoginTwoFactorPrompt() {
        val oAuthResult = LoginResult.OAuthResult("token", null, null, null)
        doAnswer { invocation ->
            val callback = invocation.getArgument<LoginCallback>(5)
            callback.twoFactorPrompt(oAuthResult, Throwable("2FA needed"), "token")
            null
        }.whenever(loginClient).doLogin(eq("user"), eq("pass"), anyOrNull(), eq(""), any(), any())

        viewModel.performLogin("user", "pass", "")

        assertEquals(oAuthResult, viewModel.lastLoginResult)
        assertEquals(LoginState.TwoFactorPrompt(oAuthResult), viewModel.loginState.value)
    }

    @Test
    fun testPerformLoginEmailAuthPrompt() {
        val emailAuthResult = LoginResult.EmailAuthResult("token", null, null, null)
        doAnswer { invocation ->
            val callback = invocation.getArgument<LoginCallback>(5)
            callback.emailAuthPrompt(emailAuthResult, Throwable("Email auth needed"), "token")
            null
        }.whenever(loginClient).doLogin(eq("user"), eq("pass"), anyOrNull(), eq(""), any(), any())

        viewModel.performLogin("user", "pass", "")

        assertEquals(emailAuthResult, viewModel.lastLoginResult)
        assertEquals(LoginState.EmailAuthPrompt(emailAuthResult), viewModel.loginState.value)
    }

    @Test
    fun testPerformLoginPasswordResetPrompt() {
        doAnswer { invocation ->
            val callback = invocation.getArgument<LoginCallback>(5)
            callback.passwordResetPrompt("token")
            null
        }.whenever(loginClient).doLogin(eq("user"), eq("pass"), anyOrNull(), eq(""), any(), any())

        viewModel.performLogin("user", "pass", "")

        assertEquals(LoginState.PasswordResetPrompt, viewModel.loginState.value)
    }

    @Test
    fun testPerformLoginError() {
        val errorThrowable = RuntimeException("Network Error")
        doAnswer { invocation ->
            val callback = invocation.getArgument<LoginCallback>(5)
            callback.error(errorThrowable)
            null
        }.whenever(loginClient).doLogin(eq("user"), eq("pass"), anyOrNull(), eq(""), any(), any())

        viewModel.performLogin("user", "pass", "")

        assertEquals(LoginState.Error("Network Error"), viewModel.loginState.value)
    }

    @Test
    fun testSkipLogin() {
        viewModel.skipLogin()

        verify(applicationKvStore).putBoolean("login_skipped", true)
        assertEquals(LoginState.Skipped, viewModel.loginState.value)
    }

    @Test
    fun testCheckLoginStatusWhenAlreadyLoggedIn() {
        whenever(sessionManager.currentAccount).thenReturn(mock<Account>())
        whenever(sessionManager.isUserLoggedIn).thenReturn(true)

        viewModel.checkLoginStatus()

        verify(applicationKvStore).putBoolean("login_skipped", false)
        assertEquals(LoginState.AlreadyLoggedIn, viewModel.loginState.value)
    }

    @Test
    fun testCheckLoginStatusWhenSkipped() {
        whenever(sessionManager.currentAccount).thenReturn(null)
        whenever(applicationKvStore.getBoolean("login_skipped", false)).thenReturn(true)

        viewModel.checkLoginStatus()

        assertEquals(LoginState.Skipped, viewModel.loginState.value)
    }

    @Test
    fun testCheckLoginStatusWhenNeither() {
        whenever(sessionManager.currentAccount).thenReturn(null)
        whenever(applicationKvStore.getBoolean("login_skipped", false)).thenReturn(false)

        viewModel.checkLoginStatus()

        assertEquals(LoginState.Idle, viewModel.loginState.value)
    }

    @Test
    fun testIsLoginButtonEnabled() {
        // Valid
        assertTrue(viewModel.isLoginButtonEnabled("user", "pass", "123456", true))
        assertTrue(viewModel.isLoginButtonEnabled("user", "pass", null, false))

        // Invalid
        assertFalse(viewModel.isLoginButtonEnabled("", "pass", null, false))
        assertFalse(viewModel.isLoginButtonEnabled(null, "pass", null, false))
        assertFalse(viewModel.isLoginButtonEnabled("user", "", null, false))
        assertFalse(viewModel.isLoginButtonEnabled("user", null, null, false))
    }

    @Test
    fun testCancel() {
        viewModel.cancel()
        verify(loginClient).cancel()
    }

    @Test
    fun testFactoryCreatesViewModel() {
        val factory = LoginViewModel.LoginViewModelFactory(loginClient, sessionManager, applicationKvStore)
        val created = factory.create(LoginViewModel::class.java)
        assertNotNull(created)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFactoryThrowsForUnknownClass() {
        val factory = LoginViewModel.LoginViewModelFactory(loginClient, sessionManager, applicationKvStore)

        class UnknownViewModel : ViewModel()

        factory.create(UnknownViewModel::class.java)
    }
}
