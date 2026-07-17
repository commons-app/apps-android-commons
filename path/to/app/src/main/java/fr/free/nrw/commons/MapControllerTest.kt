# Import necessary libraries
import org.junit.Assert
import org.junit.Test

/**
 * Test class for MapController.
 */
class MapControllerTest {
    @Test
    fun testSortQids() {
        val mapController = MapController()
        val sortedQids = mapController.sortQids()
        Assert.assertTrue(sortedQids.joinToString() == "1,2,3,4,5")
    }

    @Test
    fun testBinarySearchQid() {
        val mapController = MapController()
        val qid = 3
        val index = mapController.binarySearchQid(qid)
        Assert.assertEquals(index, 2)
    }
}