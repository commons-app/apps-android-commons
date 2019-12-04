package fr.free.nrw.commons.auth

import com.nhaarman.mockitokotlin2.verify
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import javax.inject.Inject
import javax.inject.Named

class LogoutClientTest {

    @Mock @field:[Inject Named("commons-service")]
    internal var service: Service? = null

    @InjectMocks
    var logoutClient: LogoutClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val mwQueryResponse = mock(MwQueryResponse::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult!!.csrfToken()).thenReturn("test_token")
        `when`(mwQueryResponse.query()).thenReturn(mwQueryResult)
        `when`(service!!.csrfToken)
                .thenReturn(Observable.just(mwQueryResponse))
    }

    @Test
    fun postLogout() {
        `when`(service!!.postLogout(anyString())).thenReturn(Observable.just(mock(MwPostResponse::class.java)))
        logoutClient!!.postLogout()
        verify(service, times(1))!!.csrfToken
    }
}