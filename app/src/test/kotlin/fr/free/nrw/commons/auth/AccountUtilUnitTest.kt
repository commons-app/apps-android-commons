package fr.free.nrw.commons.auth

import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.FakeContextWrapper
import fr.free.nrw.commons.FakeContextWrapperWithException
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
        Assert.assertNotNull(accountUtil)
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserName() {
        Assert.assertEquals(AccountUtil.getUserName(context), "test@example.com")
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserNameWithException() {
        val context =
            FakeContextWrapperWithException(ApplicationProvider.getApplicationContext())
        Assert.assertEquals(AccountUtil.getUserName(context), null)
    }

    @Test
    @Throws(Exception::class)
    fun testAccount() {
        Assert.assertEquals(AccountUtil.account(context)?.name, "test@example.com")
    }

    @Test
    @Throws(Exception::class)
    fun testAccountWithException() {
        val context =
            FakeContextWrapperWithException(ApplicationProvider.getApplicationContext())
        Assert.assertEquals(AccountUtil.account(context), null)
    }
}