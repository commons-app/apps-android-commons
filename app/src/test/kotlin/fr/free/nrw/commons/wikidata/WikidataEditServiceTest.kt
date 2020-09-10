package fr.free.nrw.commons.wikidata

import android.content.Context
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.upload.WikidataPlace
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class WikidataEditServiceTest {
    @Mock
    internal lateinit var context: Context

    @Mock
    internal lateinit var directKvStore: JsonKvStore

    @Mock
    internal lateinit var wikidataClient: WikidataClient

    @Mock
    internal lateinit var wikibaseClient: WikiBaseClient

    @Mock
    internal lateinit var gson: Gson

    @InjectMocks
    lateinit var wikidataEditService: WikidataEditService

    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun noClaimsWhenLocationIsNotCorrect() {
        whenever(directKvStore.getBoolean("Picture_Has_Correct_Location", true))
            .thenReturn(false)
        wikidataEditService.createClaim(mock(), "Test.jpg", hashMapOf())
        verifyZeroInteractions(wikidataClient)
    }

    @Test
    fun noClaimsWhenEntityIdIsNull() {
        wikidataEditService!!.createClaimWithLogging(null, null,"Test.jpg","")
        verifyZeroInteractions(wikidataClient!!)
    }

    @Test
    fun noClaimsWhenFileNameIsNull() {
        wikidataEditService!!.createClaimWithLogging("Q1", "Test", null,"")
        verifyZeroInteractions(wikidataClient!!)
    }

    @Test
    fun noClaimsWhenP18IsNotEmpty() {
        wikidataEditService!!.createClaimWithLogging("Q1", "Test","Test.jpg","Previous.jpg")
        verifyZeroInteractions(wikidataClient!!)
    }

    @Test
    fun createImageClaim() {
        whenever(directKvStore.getBoolean("Picture_Has_Correct_Location", true))
            .thenReturn(true)
        whenever(wikibaseClient.getFileEntityId(any())).thenReturn(Observable.just(1L))
        whenever(wikidataClient.setClaim(any(), anyString()))
            .thenReturn(Observable.just(1L))
        val wikidataPlace: WikidataPlace = mock()
        val uploadResult = mock<UploadResult>()
        whenever(uploadResult.filename).thenReturn("file")
        wikidataEditService.createClaim(
            wikidataPlace,
            uploadResult.filename,
            hashMapOf<String, String>()
        )
    }
}
