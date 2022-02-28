package fr.free.nrw.commons.category

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import fr.free.nrw.commons.data.models.nearby.Label
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox

class CategoryEditSearchRecyclerViewAdapterUnitTest {

    private lateinit var adapter: CategoryEditSearchRecyclerViewAdapter

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var labels: ArrayList<Label>

    @Mock
    private lateinit var recyclerView: RecyclerView

    @Mock
    private lateinit var categoryClient: CategoryClient

    @Mock
    private lateinit var callback: CategoryEditSearchRecyclerViewAdapter.Callback

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        adapter =
            CategoryEditSearchRecyclerViewAdapter(
                context,
                labels,
                recyclerView,
                categoryClient,
                callback
            )
    }

    @Test
    fun testAddToCategories() {
        val categories = mutableListOf<String>()
        Whitebox.setInternalState(adapter, "categories", categories)
        val testCategories = listOf("someString")
        adapter.addToCategories(testCategories)
        assertEquals(categories.size, testCategories.size)
    }

    @Test
    fun testRemoveFromNewCategories() {
        val testCategory = "someString"
        val newCategories = mutableListOf(testCategory)
        val originalSize = newCategories.size
        Whitebox.setInternalState(adapter, "newCategories", newCategories)
        adapter.removeFromNewCategories(testCategory)
        assertEquals(newCategories.size, originalSize - 1)
    }

    @Test
    fun testAddToNewCategories() {
        val testCategory = "someString"
        val newCategories = mutableListOf<String>()
        val originalSize = newCategories.size
        Whitebox.setInternalState(adapter, "newCategories", newCategories)
        adapter.addToNewCategories(testCategory)
        assertEquals(newCategories.size, originalSize + 1)
    }

    @Test
    fun testGetCategories() {
        val categories = mutableListOf<String>()
        Whitebox.setInternalState(adapter, "categories", categories)
        assertEquals(adapter.categories, categories)
    }

    @Test
    fun testGetNewCategories() {
        val newCategories = mutableListOf<String>()
        Whitebox.setInternalState(adapter, "newCategories", newCategories)
        assertEquals(adapter.newCategories, newCategories)
    }

}