package fr.free.nrw.commons.wikidata

import android.content.Context
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.wikidata.model.Entities
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
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
    internal lateinit var depictsEditor: DepictsEditor

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
        whenever(depictsEditor.updateDepictsProperty("12345", listOf("Q12")))
            .thenReturn(Observable.just(true))
        wikidataEditService
            .updateDepictsProperty("12345", listOf("Q12"))
            .test()
            .assertValue(true)
            .assertComplete()
        org.mockito.Mockito
            .verify(depictsEditor)
            .updateDepictsProperty("12345", listOf("Q12"))
    }

    /** Two uploads from the same bookmark must read the live image claim each time. */
    @Test
    fun staleBookmarkDoesNotAddSecondImageClaim() {
        whenever(directKvStore.getBoolean("Picture_Has_Correct_Location", true))
            .thenReturn(true)
        val wikidataPlace: WikidataPlace = mock()
        whenever(wikidataPlace.id).thenReturn("Q42")
        whenever(wikidataClient.getItemForImageEdit("Q42"))
            .thenReturn(itemWithClaims("{}", 42), itemWithClaims("""{"P18":[{}]}""", 43))
        whenever(wikidataClient.setClaim(any(), any(), eq(42L))).thenReturn(Observable.just(43L))

        assertEquals(
            ImageClaimResult(revisionId = 43L),
            wikidataEditService.createClaim(wikidataPlace, "First.jpg", mapOf("en" to "Caption")),
        )
        assertEquals(
            ImageClaimResult(skipped = true),
            wikidataEditService.createClaim(wikidataPlace, "Second.jpg", emptyMap()),
        )
        verify(wikidataClient).setClaim(any(), any(), eq(42L))
        verify(wikidataClient, times(2)).getItemForImageEdit("Q42")
        verifyNoMoreInteractions(wikidataClient)
    }

    /** A failed lookup must prevent a claim write. */
    @Test
    fun failedLookupDoesNotWriteImageClaim() {
        whenever(directKvStore.getBoolean("Picture_Has_Correct_Location", true)).thenReturn(true)
        val place: WikidataPlace = mock()
        whenever(place.id).thenReturn("Q42")
        whenever(wikidataClient.getItemForImageEdit("Q42")).thenThrow(IllegalStateException("No entity"))

        assertThrows(IllegalStateException::class.java) {
            wikidataEditService.createClaim(place, "Second.jpg", emptyMap())
        }
        verify(wikidataClient, never()).setClaim(any(), any(), any())
    }

    /** Deserializes the claims and revision fields as returned by Wikidata. */
    private fun itemWithClaims(
        claims: String,
        revision: Long,
    ): Entities.Entity =
        Gson().fromJson(
            """{"id":"Q42","lastrevid":$revision,"claims":$claims}""",
            Entities.Entity::class.java,
        )
}
