package fr.free.nrw.commons.auth

import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.FakeContextWrapper
import fr.free.nrw.commons.FakeContextWrapperWithException
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.equalTo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class AccountUtilUnitTest {

    private lateinit var context: FakeContextWrapper
    private lateinit var accountUtil: AccountUtil

    @Before
    @Throws(Exception::class)
    fun setUp() {
        context = FakeContextWrapper(ApplicationProvider.getApplicationContext())
        accountUtil = AccountUtil()
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        assertThat(accountUtil, notNullValue())
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserName() {
        assertThat(AccountUtil.getUserName(context), equalTo( "test@example.com"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserNameWithException() {
        val context =
            FakeContextWrapperWithException(ApplicationProvider.getApplicationContext())
        assertThat(AccountUtil.getUserName(context), equalTo( null))
    }

    @Test
    @Throws(Exception::class)
    fun testAccount() {
        assertThat(AccountUtil.account(context)?.name, equalTo( "test@example.com"))
    }

    @Test
    @Throws(Exception::class)
    fun testAccountWithException() {
        val context =
            FakeContextWrapperWithException(ApplicationProvider.getApplicationContext())
        assertThat(AccountUtil.account(context), equalTo( null))
    }
}