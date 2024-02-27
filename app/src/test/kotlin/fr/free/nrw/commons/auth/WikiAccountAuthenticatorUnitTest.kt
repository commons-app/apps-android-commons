package fr.free.nrw.commons.auth

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.equalTo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class WikiAccountAuthenticatorUnitTest {

    private lateinit var context: Context
    private lateinit var authenticator: WikiAccountAuthenticator

    @Mock
    private lateinit var response: AccountAuthenticatorResponse

    @Mock
    private lateinit var account: Account

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        authenticator = WikiAccountAuthenticator(context)
    }

    @Test
    fun checkNotNull() {
        assertThat(authenticator, notNullValue())
    }

    @Test
    fun testEditProperties() {
        val bundle: Bundle = authenticator.editProperties(response, "test")
        assertThat(bundle.getString("test"), equalTo( "editProperties"))
    }

    @Test
    fun testAddAccountCaseNotSupportedType() {
        val bundle: Bundle = authenticator.addAccount(response, "test", null, null, null)
        assertThat(bundle.getString("test"), equalTo( "addAccount"))
    }

    @Test
    fun testAddAccountCaseSupportedType() {
        val bundle: Bundle =
            authenticator.addAccount(response, BuildConfig.ACCOUNT_TYPE, null, null, null)
        val intent: Intent? = bundle.getParcelable(AccountManager.KEY_INTENT)
        assertThat(
            intent?.extras!![AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE],
            equalTo(response)
        )
    }

    @Test
    fun testConfirmCredentials() {
        val bundle: Bundle = authenticator.confirmCredentials(response, account, null)
        assertThat(bundle.getString("test"), equalTo( "confirmCredentials"))
    }

    @Test
    fun testGetAuthToken() {
        val bundle: Bundle = authenticator.getAuthToken(response, account, "", null)
        assertThat(bundle.getString("test"), equalTo( "getAuthToken"))
    }

    @Test
    fun testGetAuthTokenLabelCaseNull() {
        assertThat(authenticator.getAuthTokenLabel(""), equalTo( null))
    }

    @Test
    fun testGetAuthTokenLabelCaseNonNull() {
        assertThat(
            authenticator.getAuthTokenLabel(BuildConfig.ACCOUNT_TYPE),
            equalTo(AccountUtil.AUTH_TOKEN_TYPE)
        )
    }

    @Test
    fun testUpdateCredentials() {
        val bundle: Bundle? = authenticator.updateCredentials(response, account, null, null)
        assertThat(bundle?.getString("test"), equalTo( "updateCredentials"))
    }

    @Test
    fun testHasFeatures() {
        val bundle: Bundle? = authenticator.hasFeatures(response, account, arrayOf(""))
        assertThat(bundle?.getBoolean(AccountManager.KEY_BOOLEAN_RESULT), equalTo( false))
    }

    @Test
    fun testGetAccountRemovalAllowed() {
        val bundle: Bundle? = authenticator.getAccountRemovalAllowed(response, account)
        assertThat(bundle?.getBoolean(AccountManager.KEY_BOOLEAN_RESULT), equalTo( true))
    }

}