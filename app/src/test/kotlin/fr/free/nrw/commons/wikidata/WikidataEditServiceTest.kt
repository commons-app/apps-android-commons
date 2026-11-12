package fr.free.nrw.commons.wikidata

import android.content.Context
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.upload.UploadResult
import fr.free.nrw.commons.upload.WikidataPlace
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.wikidata.model.Entities
import io.reactivex.Observable
import media
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import java.io.IOException

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

    private fun contributionWith(location: DepictedItem?) =
        Contribution(
            media = media(captions = emptyMap()),
            depictedItems = listOf(depictedItem(id = "Q1")),
            locationOfCreation = location,
        )

    private fun stubFileEntityId() {
        whenever(wikibaseClient.getFileEntityId(anyOrNull())).thenReturn(Observable.just(42L))
        whenever(depictsEditor.addDepictsProperty("42", listOf("Q1")))
            .thenReturn(Observable.just(true))
    }

    @Test
    fun `location of creation is saved as its own statement after the depictions`() {
        stubFileEntityId()
        whenever(depictsEditor.addLocationOfCreationProperty("42", "Q12439"))
            .thenReturn(Observable.just(true))

        wikidataEditService
            .addDepictionsAndCaptions(
                UploadResult("Success", "key", 0, "file.jpg"),
                contributionWith(depictedItem(id = "Q12439")),
            ).test()
            .assertValues(true, true)
            .assertComplete()

        val inOrder = inOrder(depictsEditor)
        inOrder.verify(depictsEditor).addDepictsProperty("42", listOf("Q1"))
        inOrder.verify(depictsEditor).addLocationOfCreationProperty("42", "Q12439")
    }

    @Test
    fun `item that is both depicted and the shooting location gets both statements`() {
        whenever(wikibaseClient.getFileEntityId(anyOrNull())).thenReturn(Observable.just(42L))
        whenever(depictsEditor.addDepictsProperty("42", listOf("Q12439")))
            .thenReturn(Observable.just(true))
        whenever(depictsEditor.addLocationOfCreationProperty("42", "Q12439"))
            .thenReturn(Observable.just(true))

        wikidataEditService
            .addDepictionsAndCaptions(
                UploadResult("Success", "key", 0, "file.jpg"),
                Contribution(
                    media = media(captions = emptyMap()),
                    depictedItems = listOf(depictedItem(id = "Q12439")),
                    locationOfCreation = depictedItem(id = "Q12439"),
                ),
            ).test()
            .assertValues(true, true)
            .assertComplete()

        verify(depictsEditor).addDepictsProperty("42", listOf("Q12439"))
        verify(depictsEditor).addLocationOfCreationProperty("42", "Q12439")
    }

    @Test
    fun `place used only as shooting location is not sent as a depiction`() {
        whenever(wikibaseClient.getFileEntityId(anyOrNull())).thenReturn(Observable.just(42L))
        whenever(depictsEditor.addDepictsProperty("42", listOf("Q_ENGINE")))
            .thenReturn(Observable.just(true))
        whenever(depictsEditor.addLocationOfCreationProperty("42", "Q12439"))
            .thenReturn(Observable.just(true))

        wikidataEditService
            .addDepictionsAndCaptions(
                UploadResult("Success", "key", 0, "file.jpg"),
                Contribution(
                    media = media(captions = emptyMap()),
                    depictedItems = listOf(depictedItem(id = "Q_ENGINE")),
                    locationOfCreation = depictedItem(id = "Q12439"),
                ),
            ).test()
            .assertComplete()

        verify(depictsEditor).addDepictsProperty("42", listOf("Q_ENGINE"))
        verify(depictsEditor, never()).addDepictsProperty("42", listOf("Q12439"))
        verify(depictsEditor).addLocationOfCreationProperty("42", "Q12439")
    }

    @Test
    fun `no location of creation statement is saved when none was chosen`() {
        stubFileEntityId()

        wikidataEditService
            .addDepictionsAndCaptions(
                UploadResult("Success", "key", 0, "file.jpg"),
                contributionWith(null),
            ).test()
            .assertValues(true)
            .assertComplete()

        verify(depictsEditor, never()).addLocationOfCreationProperty(any(), any())
    }

    @Test
    fun `failing to save the location of creation does not fail the upload`() {
        stubFileEntityId()
        whenever(depictsEditor.addLocationOfCreationProperty("42", "Q12439"))
            .thenReturn(Observable.error(IOException("offline")))

        wikidataEditService
            .addDepictionsAndCaptions(
                UploadResult("Success", "key", 0, "file.jpg"),
                contributionWith(depictedItem(id = "Q12439")),
            ).test()
            .assertValues(true, false)
            .assertComplete()
            .assertNoErrors()
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
