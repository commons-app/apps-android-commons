package fr.free.nrw.commons.auth

import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.equalTo
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
        assertThat(service, notNullValue())
    }

    @Test
    fun testOnBindCaseNull() {
        val field: Field =
            WikiAccountAuthenticatorService::class.java.getDeclaredField("authenticator")
        field.isAccessible = true
        field.set(service, null)
        assertThat(service.onBind(null), equalTo( null))
    }

}