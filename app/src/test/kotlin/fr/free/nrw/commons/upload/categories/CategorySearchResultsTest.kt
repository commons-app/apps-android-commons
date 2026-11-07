package fr.free.nrw.commons.upload.categories

import categoryItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import media
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class CategorySearchResultsTest {
    @Test
    fun `upload search filters spam but preserves exact matches and nonempty captions`() {
        val repository = mock<UploadRepository>()
        val upload = mock<UploadItem>()
        whenever(upload.uploadMediaDetails).thenReturn(mutableListOf(UploadMediaDetail(captionText = "Title")))
        val untitled = mock<UploadItem>()
        whenever(untitled.uploadMediaDetails).thenReturn(mutableListOf(UploadMediaDetail(captionText = "")))
        whenever(repository.getUploads()).thenReturn(listOf(upload, untitled))
        val exact = categoryItem("2026")
        val spam = categoryItem("2025")
        val ordinary = categoryItem("Trees")
        whenever(repository.searchAll(any(), any(), any()))
            .thenReturn(Observable.just(listOf(exact, spam, ordinary)))
        whenever(repository.isSpammyCategory("2026")).thenReturn(true)
        whenever(repository.isSpammyCategory("2025")).thenReturn(true)

        CategorySearchResults(repository, Schedulers.trampoline())
            .searchResults("2026", null)
            .test()
            .assertValue(listOf(exact, ordinary))
            .assertComplete()
            .assertNoErrors()

        verify(repository).searchAll("2026", listOf("Title"), repository.getSelectedDepictions())
    }

    @Test
    fun `edit search marks existing categories selected and excludes hidden results`() {
        val repository = mock<UploadRepository>()
        whenever(repository.getSelectedExistingCategories()).thenReturn(listOf("Existing", "Hidden"))
        whenever(repository.getCategories(any())).thenReturn(
            Observable.just(listOf(categoryItem("Existing"), categoryItem("Hidden", thumbUrl = "hidden"))),
        )
        val suggestion = categoryItem("Trees")
        whenever(repository.searchAll(any(), any(), any())).thenReturn(
            Observable.just(listOf(suggestion, categoryItem("Hidden suggestion", thumbUrl = "hidden"))),
        )

        CategorySearchResults(repository, Schedulers.trampoline())
            .searchResults("Trees", media())
            .test()
            .assertValue { items ->
                items.map { it.name } == listOf("Existing", "Trees") &&
                    items[0].isSelected &&
                    !items[1].isSelected
            }.assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `search propagates repository errors`() {
        val repository = mock<UploadRepository>()
        val error = IllegalStateException("Search failed")
        whenever(repository.searchAll(any(), any(), any())).thenReturn(Observable.error(error))

        CategorySearchResults(repository, Schedulers.trampoline())
            .searchResults("Trees", null)
            .test()
            .assertError(error)
    }
}
