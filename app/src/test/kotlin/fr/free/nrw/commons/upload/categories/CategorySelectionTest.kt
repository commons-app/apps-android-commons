package fr.free.nrw.commons.upload.categories

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import categoryItem
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.repository.UploadRepository
import media
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Verifies immediate category selection updates independently of presenter attachment. */
class CategorySelectionTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /** New selections take precedence over duplicate search results while retaining other results. */
    @Test
    fun `selection updates displayed list distinctly and passes edited media to repository`() {
        val repository = mock<UploadRepository>()
        val media = media()
        val selected = categoryItem("Selected")
        val other = categoryItem("Other")
        val categories = MutableLiveData<List<CategoryItem>>(listOf(categoryItem("Selected"), other))

        CategorySelection(repository).selectNewCategories(listOf(selected), media, categories)

        assertTrue(selected.isSelected)
        assertEquals(listOf("Selected", "Other"), categories.value!!.map { it.name })
        assertSame(selected, categories.value!![0])
        verify(repository).onCategoryClicked(selected, media)
    }

    /** Selection still reaches the repository before any search result list has been initialized. */
    @Test
    fun `selection before search leaves uninitialized display unchanged`() {
        val repository = mock<UploadRepository>()
        val selected = categoryItem("Selected")
        val categories = MutableLiveData<List<CategoryItem>>()

        CategorySelection(repository).selectNewCategories(listOf(selected), null, categories)

        assertTrue(selected.isSelected)
        assertEquals(null, categories.value)
        verify(repository).onCategoryClicked(selected, null)
    }
}
