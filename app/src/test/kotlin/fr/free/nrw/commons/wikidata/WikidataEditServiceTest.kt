package fr.free.nrw.commons.wikidata

import android.content.Context
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.wikidata.model.AddEditTagResponse
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class WikidataEditServiceTest {
    @Mock
    internal var context: Context? = null
    @Mock
    internal var wikidataEditListener: WikidataEditListener? = null
    @Mock
    internal var directKvStore: JsonKvStore? = null
    @Mock
    internal var wikidataClient: WikidataClient? = null

    @InjectMocks
    var wikidataEditService: WikidataEditService? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun noClaimsWhenEntityIdIsNull() {
        wikidataEditService!!.createClaimWithLogging(null, "Test.jpg")
        verifyZeroInteractions(wikidataClient!!)
    }

    @Test
    fun noClaimsWhenFileNameIsNull() {
        wikidataEditService!!.createClaimWithLogging("Q1", null)
        verifyZeroInteractions(wikidataClient!!)
    }

    @Test
    fun noClaimsWhenLocationIsNotCorrect() {
        `when`(directKvStore!!.getBoolean("Picture_Has_Correct_Location", true))
                .thenReturn(false)
        wikidataEditService!!.createClaimWithLogging("Q1", "Test.jpg")
        verifyZeroInteractions(wikidataClient!!)
    }

    @Test
    fun createClaimWithLogging() {
        `when`(directKvStore!!.getBoolean("Picture_Has_Correct_Location", true))
                .thenReturn(true)
        `when`(wikidataClient!!.createClaim(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(1L))
        `when`(wikidataClient!!.addEditTag(anyLong(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(Observable.just(mock(AddEditTagResponse::class.java)))
        wikidataEditService!!.createClaimWithLogging("Q1", "Test.jpg")
        verify(wikidataClient!!, times(1))
                .createClaim(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())
    }
}