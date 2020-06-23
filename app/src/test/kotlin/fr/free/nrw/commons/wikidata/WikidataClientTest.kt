package fr.free.nrw.commons.wikidata

import com.nhaarman.mockitokotlin2.mock
import fr.free.nrw.commons.wikidata.model.AddEditTagResponse
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
}
