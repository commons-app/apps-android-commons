package fr.free.nrw.commons.category

import categoryItem
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import depictedItem
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Verifies that unavailable depiction categories do not discard other upload suggestions. */
class CategoriesModelDepictionTest {
    private val categoryClient: CategoryClient = mock()
    private val categoryDao: CategoryDao = mock()
    private val gpsCategoryModel: GpsCategoryModel = mock()
    private lateinit var model: CategoriesModel

    /** Keeps independent suggestion sources empty unless a test supplies them explicitly. */
    @Before
    fun setUp() {
        whenever(gpsCategoryModel.categoriesFromLocation)
            .thenReturn(BehaviorSubject.createDefault(emptyList<CategoryItem>()))
        whenever(categoryDao.recentCategories(CategoriesModel.SEARCH_CATS_LIMIT)).thenReturn(emptyList())
        model = CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
    }

    /** A missing named category must produce an empty suggestion list rather than terminate loading. */
    @Test
    fun emptyCategoryLookupProducesNoSuggestionsWithoutError() {
        stubLookup("Missing", emptyList())

        model
            .searchAll("", emptyList(), listOf(depictedItem(commonsCategories = listOf(categoryItem("Missing")))))
            .test()
            .assertValue(emptyList())
            .assertNoErrors()
    }

    /** Missing entries before and after a valid category must not stop the rest of that depiction. */
    @Test
    fun missingCategoriesDoNotDiscardValidCategoriesInTheSameDepiction() {
        stubLookup("Missing first", emptyList())
        stubLookup("Available", listOf(categoryItem("Available")))
        stubLookup("Missing last", emptyList())
        val depiction =
            depictedItem(
                commonsCategories = listOf("Missing first", "Available", "Missing last").map { categoryItem(it) },
            )

        model
            .searchAll("", emptyList(), listOf(depiction))
            .test()
            .assertValue(listOf(categoryItem("Available")))
            .assertNoErrors()

        verify(categoryClient).getCategoriesByName("Missing last", "Missing last", CategoriesModel.SEARCH_CATS_LIMIT)
    }

    /** Other depictions, P18, location, title and recent suggestions must survive a missing lookup. */
    @Test
    fun missingDepictionCategoryPreservesOtherSuggestionSources() {
        stubLookup("Missing", emptyList())
        stubLookup("Available", listOf(categoryItem("Available")))
        whenever(categoryClient.getCategoriesOfImage("image.jpg", CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(listOf(categoryItem("From image"))))
        whenever(gpsCategoryModel.categoriesFromLocation)
            .thenReturn(BehaviorSubject.createDefault(listOf(categoryItem("From location"))))
        whenever(categoryClient.searchCategories("Photo title", CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(listOf(categoryItem("From title"))))
        whenever(categoryDao.recentCategories(CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(listOf(categoryItem("Recent")))
        val depictions =
            listOf(
                depictedItem(commonsCategories = listOf(categoryItem("Missing"))),
                depictedItem(commonsCategories = listOf(categoryItem("Available"))),
                depictedItem(imageUrl = "image.jpg"),
            )

        model
            .searchAll("", listOf("Photo title"), depictions)
            .test()
            .assertValue(
                listOf("Available", "From image", "From location", "From title", "Recent").map { categoryItem(it) },
            ).assertNoErrors()
    }

    /** Retains the existing first-match behavior and metadata when the lookup is nonempty. */
    @Test
    fun nonemptyLookupPreservesFirstMatchAndMetadata() {
        val first = categoryItem("First match", "Description", "thumbnail.jpg", selected = true)
        stubLookup("Requested", listOf(first, categoryItem("Second match")))

        val observer =
            model
                .searchAll("", emptyList(), listOf(depictedItem(commonsCategories = listOf(categoryItem("Requested")))))
                .test()
                .assertValue(listOf(categoryItem("First match")))
                .assertNoErrors()
        val actual = observer.values().single().single()
        assertEquals(first.description, actual.description)
        assertEquals(first.thumbnail, actual.thumbnail)
        assertEquals(false, actual.isSelected)
    }

    /** Supplies a named lookup without changing the request limit or default offset. */
    private fun stubLookup(
        name: String,
        result: List<CategoryItem>,
    ) {
        whenever(categoryClient.getCategoriesByName(name, name, CategoriesModel.SEARCH_CATS_LIMIT))
            .thenReturn(Single.just(result))
    }
}
