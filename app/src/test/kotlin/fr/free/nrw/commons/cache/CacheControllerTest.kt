package fr.free.nrw.commons.cache

import com.github.varunpant.quadtree.Point
import com.github.varunpant.quadtree.QuadTree
import fr.free.nrw.commons.caching.CacheController
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class CacheControllerTest {
    /**
     * initial setup, test environment
     */
    private lateinit var cacheController: CacheController

    @Mock
    private lateinit var quadTree: QuadTree<List<String>>

    private lateinit var points: Array<Point<List<String>>>

    var value = ArrayList<String>()


    @Before
    @Throws(Exception::class)
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val point = Point<List<String>>(1.0, 1.0, value)
        points = arrayOf(point)
        value.add("1")
        cacheController = CacheController(quadTree)
        Mockito.`when`(quadTree.searchWithin(ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble())).thenReturn(points)
    }

    /**
     * Test find category
     */
    @Test
    fun testFindCategory() {
        val findCategory = cacheController.findCategory()
        verify(quadTree).searchWithin(ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble(), ArgumentMatchers.anyDouble())
        assert(findCategory.size == 1)
    }
}