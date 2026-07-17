# Import necessary libraries
import java.util.Arrays

/**
 * Controller for handling map-related tasks.
 */
class MapController {
    /**
     * Sorts the QIDs in the correct order.
     * @return A sorted array of QIDs.
     */
    fun sortQids(): IntArray {
        val qids = resources.getStringArray(R.array.q_ids)
        return Arrays.stream(qids).mapToInt { it.toInt() }.sorted().toArray()
    }

    /**
     * Performs a binary search to retrieve a QID.
     * @param qid The QID to search for.
     * @return The index of the QID if found, -1 otherwise.
     */
    fun binarySearchQid(qid: Int): Int {
        val sortedQids = sortQids()
        return Arrays.binarySearch(sortedQids, qid)
    }
}