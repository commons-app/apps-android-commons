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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

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
        Assert.assertNotNull(authenticator)
    }

    @Test
    fun testEditProperties() {
        val bundle: Bundle = authenticator.editProperties(response, "test")
        Assert.assertEquals(bundle.getString("test"), "editProperties")
    }

    @Test
    fun testAddAccountCaseNotSupportedType() {
        val bundle: Bundle = authenticator.addAccount(response, "test", null, null, null)
        Assert.assertEquals(bundle.getString("test"), "addAccount")
    }

    @Test
    fun testAddAccountCaseSupportedType() {
        val bundle: Bundle =
            authenticator.addAccount(response, BuildConfig.ACCOUNT_TYPE, null, null, null)
        val intent: Intent? = bundle.getParcelable(AccountManager.KEY_INTENT)
        Assert.assertEquals(
            intent?.extras!![AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE],
            response
        )
    }

    @Test
    fun testConfirmCredentials() {
        val bundle: Bundle = authenticator.confirmCredentials(response, account, null)
        Assert.assertEquals(bundle.getString("test"), "confirmCredentials")
    }

    @Test
    fun testGetAuthToken() {
        val bundle: Bundle = authenticator.getAuthToken(response, account, "", null)
        Assert.assertEquals(bundle.getString("test"), "getAuthToken")
    }

    @Test
    fun testGetAuthTokenLabelCaseNull() {
        Assert.assertEquals(authenticator.getAuthTokenLabel(""), null)
    }

    @Test
    fun testGetAuthTokenLabelCaseNonNull() {
        Assert.assertEquals(
            authenticator.getAuthTokenLabel(BuildConfig.ACCOUNT_TYPE),
            AccountUtil.AUTH_TOKEN_TYPE
        )
    }

    @Test
    fun testUpdateCredentials() {
        val bundle: Bundle? = authenticator.updateCredentials(response, account, null, null)
        Assert.assertEquals(bundle?.getString("test"), "updateCredentials")
    }

    @Test
    fun testHasFeatures() {
        val bundle: Bundle? = authenticator.hasFeatures(response, account, arrayOf(""))
        Assert.assertEquals(bundle?.getBoolean(AccountManager.KEY_BOOLEAN_RESULT), false)
    }

    @Test
    fun testGetAccountRemovalAllowed() {
        val bundle: Bundle? = authenticator.getAccountRemovalAllowed(response, account)
        Assert.assertEquals(bundle?.getBoolean(AccountManager.KEY_BOOLEAN_RESULT), true)
    }

}