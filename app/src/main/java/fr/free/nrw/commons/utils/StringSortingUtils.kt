package fr.free.nrw.commons.utils

import fr.free.nrw.commons.category.CategoryItem
import java.lang.Math.signum
import java.util.Comparator


object StringSortingUtils {

    /**
     * Returns Comparator for sorting strings by their similarity to the filter.
     * By using this Comparator we get results
     * from the highest to the lowest similarity with the filter.
     *
     * @param filter String to compare similarity with
     * @return Comparator with string similarity
     */
    @JvmStatic
    fun sortBySimilarity(filter: String): Comparator<CategoryItem> {
        return Comparator { firstItem, secondItem ->
            val firstItemSimilarity = calculateSimilarity(firstItem.name, filter)
            val secondItemSimilarity = calculateSimilarity(secondItem.name, filter)
            signum(secondItemSimilarity - firstItemSimilarity).toInt()
        }
    }

    /**
     * Determines String similarity between str1 and str2 on scale from 0.0 to 1.0
     * @param str1 String 1
     * @param str2 String 2
     * @return Double between 0.0 and 1.0 that reflects string similarity
     */
    private fun calculateSimilarity(str1: String, str2: String): Double {
        val longerLength = maxOf(str1.length, str2.length)

        if (longerLength == 0) return 1.0

        val distanceBetweenStrings = levenshteinDistance(str1, str2)
        return (longerLength - distanceBetweenStrings) / longerLength.toDouble()
    }

    /**
     * Levenshtein distance algorithm
     * https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     *
     * @param str1 String 1
     * @param str2 String 2
     * @return Number of characters the strings differ by
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        if (str1 == str2) return 0
        if (str1.isEmpty()) return str2.length
        if (str2.isEmpty()) return str1.length

        var cost = IntArray(str1.length + 1) { it }
        var newCost = IntArray(str1.length + 1)

        // transformation cost for each letter in str2
        for (j in 1..str2.length) {
            // initial cost of skipping prefix in String str2
            newCost[0] = j

            // transformation cost for each letter in str1
            for (i in 1..str1.length) {
                // matching current letters in both strings
                val match = if (str1[i - 1] == str2[j - 1]) 0 else 1

                // computing cost for each transformation
                val costReplace = cost[i - 1] + match
                val costInsert = cost[i] + 1
                val costDelete = newCost[i - 1] + 1

                // keep minimum cost
                newCost[i] = minOf(costInsert, costDelete, costReplace)
            }

            // swap cost arrays
            val tmp = cost
            cost = newCost
            newCost = tmp
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[str1.length]
    }
}
