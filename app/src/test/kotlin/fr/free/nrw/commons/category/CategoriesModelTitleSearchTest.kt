package fr.free.nrw.commons.category

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.upload.GpsCategoryModel
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.subjects.BehaviorSubject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies that caption based suggestions search a caption shared by several pictures once, and
 * search different captions up to 3 at a time.
 */
class CategoriesModelTitleSearchTest {
    private val categoryClient = mock<CategoryClient>()
    private val categoryDao = mock<CategoryDao>()
    private val gpsCategoryModel = mock<GpsCategoryModel>()
    private lateinit var model: CategoriesModel

    @Before
    fun setUp() {
        // Searches run on the io scheduler, which another test may have left replaced
        RxJavaPlugins.reset()
        whenever(gpsCategoryModel.categoriesFromLocation).thenReturn(BehaviorSubject.createDefault(emptyList()))
        whenever(categoryDao.recentCategories(CategoriesModel.SEARCH_CATS_LIMIT)).thenReturn(emptyList())
        whenever(categoryClient.searchCategories(anyString(), anyInt(), anyInt()))
            .thenReturn(Single.just(listOf(CategoryItem("Olive trees", "", "", false))))
        model = CategoriesModel(categoryClient, categoryDao, gpsCategoryModel)
    }

    @Test
    fun `the same caption on 20 pictures is searched once`() {
        model.searchAll("", List(20) { "Olive tree" }, emptyList()).test().awaitCount(1)

        verify(categoryClient, times(1)).searchCategories("Olive tree", CategoriesModel.SEARCH_CATS_LIMIT, 0)
    }

    @Test
    fun `different captions are each searched`() {
        model.searchAll("", listOf("Olive tree", "Villa Borghese", "Olive tree"), emptyList()).test().awaitCount(1)

        verify(categoryClient, times(1)).searchCategories("Olive tree", CategoriesModel.SEARCH_CATS_LIMIT, 0)
        verify(categoryClient, times(1)).searchCategories("Villa Borghese", CategoriesModel.SEARCH_CATS_LIMIT, 0)
    }

    @Test
    fun `different captions are searched up to 3 at a time`() {
        val running = AtomicInteger()
        val mostAtOnce = AtomicInteger()
        // Opens only when 3 searches are running at the same moment
        val threeAtOnce = CyclicBarrier(3)
        whenever(categoryClient.searchCategories(anyString(), anyInt(), anyInt())).thenAnswer {
            Single.fromCallable {
                mostAtOnce.accumulateAndGet(running.incrementAndGet(), ::maxOf)
                // Times out when fewer than 3 run at once, so the test fails instead of hanging
                runCatching { threeAtOnce.await(2, TimeUnit.SECONDS) }.onFailure { threeAtOnce.reset() }
                running.decrementAndGet()
                listOf(CategoryItem(it.getArgument(0), "", "", false))
            }
        }

        model
            .searchAll("", (1..6).map { "Caption $it" }, emptyList())
            .test()
            .awaitCount(1)
            .assertValue { found -> found.map { it.name } == (1..6).map { "Caption $it" } }
        assertEquals(3, mostAtOnce.get())
    }
}
