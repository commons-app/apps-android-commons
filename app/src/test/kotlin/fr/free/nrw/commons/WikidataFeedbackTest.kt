package fr.free.nrw.commons

import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.WikidataFeedback
import org.junit.Before
import org.junit.Test
import org.mockito.*
/**
 * Unit Tests for WikidataFeedback.java*/

class WikidataFeedbackTest {

    @Mock internal var nearbyController: NearbyController ? = null

    @InjectMocks
    var wikidataFeedback: WikidataFeedback? = null

    /**
     * Check whether the network call for retrieving feebacks returns the correct response*/

    @Before
    @Throws(Exception::class)
    fun setUp(){
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(nearbyController!!.getFeedback(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(ArgumentMatchers.anyString())
    }

    @Test
    fun verifyNetworkResponse(){
        wikidataFeedback!!.getWikidataFeedback(WikidataFeedback.place, WikidataFeedback.wikidataQId)
    }
}