package fr.free.nrw.commons.wikidata

import fr.free.nrw.commons.wikidata.model.AddEditTagResponse
import fr.free.nrw.commons.wikidata.model.WbCreateClaimResponse
import io.reactivex.Observable
import okhttp3.RequestBody
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult

class WikidataClientTest {

    @Mock
    internal var wikidataInterface: WikidataInterface? = null

    @InjectMocks
    var wikidataClient: WikidataClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val mwQueryResponse = mock(MwQueryResponse::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult!!.csrfToken()).thenReturn("test_token")
        `when`(mwQueryResponse.query()).thenReturn(mwQueryResult)
        `when`(wikidataInterface!!.getCsrfToken())
                .thenReturn(Observable.just(mwQueryResponse))
    }

    @Test
    fun createClaim() {
        `when`(wikidataInterface!!.postCreateClaim(any(RequestBody::class.java),
                any(RequestBody::class.java),
                any(RequestBody::class.java),
                any(RequestBody::class.java),
                any(RequestBody::class.java),
                any(RequestBody::class.java)))
                .thenReturn(Observable.just(mock(WbCreateClaimResponse::class.java)))
        wikidataClient!!.createClaim("Q1", "test.jpg")
    }

    @Test
    fun addEditTag() {
        `when`(wikidataInterface!!.addEditTag(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Observable.just(mock(AddEditTagResponse::class.java)))
        wikidataClient!!.addEditTag(1L, "test", "test")
    }
}