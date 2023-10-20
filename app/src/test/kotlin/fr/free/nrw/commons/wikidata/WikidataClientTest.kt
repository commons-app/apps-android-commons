package fr.free.nrw.commons.wikidata

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.wikidata.model.PageInfo
import fr.free.nrw.commons.wikidata.model.WbCreateClaimResponse
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.mwapi.MwQueryResult
import org.wikipedia.wikidata.Statement_partial

class WikidataClientTest {

    @Mock
    internal var wikidataInterface: WikidataInterface? = null

    @Mock
    internal var gson: Gson? = null

    @InjectMocks
    var wikidataClient: WikidataClient? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val mwQueryResponse = mock(MwQueryResponse::class.java)
        val mwQueryResult = mock(MwQueryResult::class.java)
        `when`(mwQueryResult!!.csrfToken()).thenReturn("test_token")
        `when`(mwQueryResponse.query()).thenReturn(mwQueryResult)
        `when`(wikidataInterface!!.getCsrfToken())
            .thenReturn(Observable.just(mwQueryResponse))
    }

    @Test
    fun addEditTag() {
        val response = mock(WbCreateClaimResponse::class.java)
        val pageInfo = mock(PageInfo::class.java)
        whenever(pageInfo.lastrevid).thenReturn(1)
        whenever(response.pageinfo).thenReturn(pageInfo)
        `when`(wikidataInterface!!.postSetClaim(anyString(), anyString(), anyString()))
            .thenReturn(Observable.just(response))
        whenever(gson!!.toJson(any(Statement_partial::class.java))).thenReturn("claim")
        val request = mock(Statement_partial::class.java)

        val claim = wikidataClient!!.setClaim(request, "test").test()
            .assertValue(1L)
    }
}
