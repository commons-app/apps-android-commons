package fr.free.nrw.commons.upload.categories

import categoryItem
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.repository.UploadRepository
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail
import io.reactivex.Observable
import io.reactivex.Single
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
    fun `next page skips a page that the spam filter removed completely`() {
        val repository = mock<UploadRepository>()
        whenever(repository.searchMore("Cats"))
            .thenReturn(Single.just(listOf(categoryItem("Cats 1890"))), Single.just(listOf(categoryItem("Black cats"))))
        whenever(repository.isSpammyCategory("Cats 1890")).thenReturn(true)

        CategorySearchResults(repository, Schedulers.trampoline())
            .moreResults("Cats")
            .test()
            .assertValue(listOf(categoryItem("Black cats")))
    }

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
        verify(repository, never()).getLocationOfCreation()
    }

    @Test
    fun `photo location suggests its Commons category separately from depictions`() {
        val repository = mock<UploadRepository>()
        val category = categoryItem("Ouagadougou")
        val existing = categoryItem("Roads")
        val location =
            depictedItem(
                name = "Ouaga",
                id = "Q3777",
                commonsCategories = listOf(categoryItem("Category:Ouagadougou")),
            )
        whenever(repository.getLocationOfCreation()).thenReturn(location)
        whenever(repository.searchAll("", emptyList(), emptyList()))
            .thenReturn(Observable.just(listOf(existing, category)))
        whenever(repository.getCategories(listOf("Ouagadougou")))
            .thenReturn(Observable.just(listOf(categoryItem("Ouagadougou", selected = true))))

        CategorySearchResults(repository, Schedulers.trampoline())
            .searchResults("", null)
            .test()
            .assertValue { it.map { item -> item.name } == listOf("Ouagadougou", "Roads") && !it.first().isSelected }
            .assertComplete()

        verify(repository).searchAll("", emptyList(), emptyList())
        verify(repository, never()).getCategories(listOf("Ouaga"))
    }

    @Test
    fun `clearing the photo location removes its suggestions on the next search`() {
        val repository = mock<UploadRepository>()
        val category = categoryItem("Ouagadougou")
        val other = categoryItem("Roads")
        whenever(repository.getLocationOfCreation()).thenReturn(depictedItem(name = "Ouagadougou"), null)
        whenever(repository.getCategories(listOf("Ouagadougou")))
            .thenReturn(Observable.just(listOf(category)))
        whenever(repository.searchAll("", emptyList(), emptyList()))
            .thenReturn(Observable.just(listOf(other)))
        val search = CategorySearchResults(repository, Schedulers.trampoline())

        search
            .searchResults("", null)
            .test()
            .assertValue(listOf(category, other))
            .assertComplete()
        search
            .searchResults("", null)
            .test()
            .assertValue(listOf(other))
            .assertComplete()

        verify(repository).getCategories(listOf("Ouagadougou"))
    }

    @Test
    fun `location lookup failure leaves existing suggestions available`() {
        val repository = mock<UploadRepository>()
        val other = categoryItem("Roads")
        whenever(repository.getLocationOfCreation()).thenReturn(depictedItem(name = "Ouagadougou"))
        whenever(repository.getCategories(listOf("Ouagadougou")))
            .thenReturn(Observable.error(IllegalStateException("Categories unavailable")))
        whenever(repository.searchAll("", emptyList(), emptyList()))
            .thenReturn(Observable.just(listOf(other)))

        CategorySearchResults(repository, Schedulers.trampoline())
            .searchResults("", null)
            .test()
            .assertValue(listOf(other))
            .assertComplete()
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

        verify(repository, never()).getLocationOfCreation()
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
