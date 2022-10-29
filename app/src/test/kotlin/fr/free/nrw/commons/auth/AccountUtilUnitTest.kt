package fr.free.nrw.commons.auth

import fr.free.nrw.commons.FakeContextWrapper
import fr.free.nrw.commons.FakeContextWrapperWithException
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.hamcrest.CoreMatchers.*;
import org.hamcrest.MatcherAssert.assertThat;


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class AccountUtilUnitTest {

    private lateinit var context: FakeContextWrapper
    private lateinit var accountUtil: AccountUtil

    @Before
    @Throws(Exception::class)
    fun setUp() {
        context = FakeContextWrapper(RuntimeEnvironment.application.applicationContext)
        accountUtil = AccountUtil()
    }

    @Test
    @Throws(Exception::class)
    fun checkNotNull() {
        assertThat(accountUtil, not(nullValue()));
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserName() {
        assertThat(AccountUtil.getUserName(context), equalTo("test@example.com"))
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserNameWithException() {
        val context =
            FakeContextWrapperWithException(RuntimeEnvironment.application.applicationContext)
        assertThat(AccountUtil.getUserName(context), not(nullValue()))
    }

    @Test
    @Throws(Exception::class)
    fun testAccount() {
        assertThat(AccountUtil.account(context)?.name, equalTo("test@example.com"))
    }

    @Test
    @Throws(Exception::class)
    fun testAccountWithException() {
        val context =
            FakeContextWrapperWithException(RuntimeEnvironment.application.applicationContext)
        assertThat(AccountUtil.account(context), not(nullValue()))
    }
}