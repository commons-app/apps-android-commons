package fr.free.nrw.commons.wikidata

import android.content.Context
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.utils.ConfigUtils.isBetaFlavour
import fr.free.nrw.commons.wikidata.model.EditClaim
import fr.free.nrw.commons.wikidata.model.RemoveClaim
import io.reactivex.Observable
import org.junit.Test
import org.mockito.Mockito.never

class DepictsEditorTest {
    private val context: Context = mock()
    private val wikiBaseClient: WikiBaseClient = mock()
    private val gson = Gson()
    private val editor = DepictsEditor(context, wikiBaseClient, gson)

    @Test
    fun `adding depictions preserves the entity id and flavour specific claims`() {
        val items = listOf("Q12", "Q234")
        val data = EditClaim.from(if (isBetaFlavour) listOf("Q10") else items, WikidataProperties.DEPICTS.propertyName)
        val json = gson.toJson(data)
        whenever(wikiBaseClient.postEditEntity("M123", json)).thenReturn(Observable.just(true))

        editor
            .addDepictsProperty("123", items)
            .test()
            .await()
            .assertValue(true)
            .assertComplete()
        verify(wikiBaseClient).postEditEntity("M123", json)
    }

    @Test
    fun `updating depictions deletes existing claims before adding replacements`() {
        val claimIds = listOf("M123\$claim")
        whenever(wikiBaseClient.getClaimIdsByProperty("M123", WikidataProperties.DEPICTS.propertyName))
            .thenReturn(Observable.just(claimIds))
        val deleteJson = gson.toJson(RemoveClaim.from(if (isBetaFlavour) listOf("Q10") else claimIds))
        whenever(wikiBaseClient.postDeleteClaims("M123", deleteJson)).thenReturn(Observable.just(true))
        whenever(wikiBaseClient.postEditEntity(eq("M123"), any())).thenReturn(Observable.just(true))

        val result = editor.updateDepictsProperty("123", listOf("Q12"))
        verify(wikiBaseClient, never()).postEditEntity(any(), any())
        result
            .test()
            .await()
            .assertValue(true)
            .assertComplete()
        verify(wikiBaseClient).postDeleteClaims("M123", deleteJson)
        verify(wikiBaseClient).postEditEntity(eq("M123"), any())
    }

    @Test
    fun `failed deletion completes without adding replacement claims`() {
        whenever(wikiBaseClient.getClaimIdsByProperty("M123", WikidataProperties.DEPICTS.propertyName))
            .thenReturn(Observable.just(emptyList()))
        whenever(wikiBaseClient.postDeleteClaims(eq("M123"), any())).thenReturn(Observable.just(false))

        editor
            .updateDepictsProperty("123", listOf("Q12"))
            .test()
            .await()
            .assertNoValues()
            .assertComplete()
            .assertNoErrors()
        verify(wikiBaseClient, never()).postEditEntity(any(), any())
    }
}
