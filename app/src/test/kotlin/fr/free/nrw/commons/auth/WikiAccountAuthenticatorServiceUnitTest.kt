package fr.free.nrw.commons.auth

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import java.lang.reflect.Field

class WikiAccountAuthenticatorServiceUnitTest {

    private lateinit var service: WikiAccountAuthenticatorService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = WikiAccountAuthenticatorService()
        service.onBind(null)
    }

    @Test
    fun checkNotNull() {
        Assert.assertNotNull(service)
    }

    @Test
    fun testOnBindCaseNull() {
        val field: Field =
            WikiAccountAuthenticatorService::class.java.getDeclaredField("authenticator")
        field.isAccessible = true
        field.set(service, null)
        Assert.assertEquals(service.onBind(null), null)
    }

}