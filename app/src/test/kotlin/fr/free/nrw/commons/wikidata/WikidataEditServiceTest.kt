package fr.free.nrw.commons.wikidata

import android.content.Context
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.wikidata.model.RemoveClaim
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verifyNoInteractions
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
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun noClaimsWhenEntityIdIsNull() {
        wikidataEditService.createClaim(mock(), "Test.jpg", hashMapOf())
        verifyNoInteractions(wikidataClient)
    }

    @Test
    fun testUpdateDepictsProperty() {
        val fileEntityId = "12345"

        whenever(wikibaseClient.getClaimIdsByProperty("M" + fileEntityId,
            WikidataProperties.DEPICTS.propertyName))
            .thenReturn(Observable.just(emptyList()))
        whenever(wikibaseClient.postDeleteClaims("M" + fileEntityId,
            gson.toJson(Mockito.mock(RemoveClaim::class.java)))
        ).thenReturn(Observable.just(true))

        wikidataEditService.updateDepictsProperty(fileEntityId, listOf())
    }

    @Test
    fun createImageClaim() {
        whenever(directKvStore.getBoolean("Picture_Has_Correct_Location", true))
            .thenReturn(true)
        whenever(wikibaseClient.getFileEntityId(anyOrNull())).thenReturn(Observable.just(1L))
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
