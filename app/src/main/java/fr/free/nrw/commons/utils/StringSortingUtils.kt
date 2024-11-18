package fr.free.nrw.commons.utils;

import fr.free.nrw.commons.category.CategoryItem;
import java.util.Comparator;

public class StringSortingUtils {

    private StringSortingUtils() {
        //no-op
    }

    /**
     * Returns Comparator for sorting strings by their similarity to the filter.
     * By using this Comparator we get results
     * from the highest to the lowest similarity with the filter.
     *
     * @param filter String to compare similarity with
     * @return Comparator with string similarity
     */
    public static Comparator<CategoryItem> sortBySimilarity(final String filter) {
        return (firstItem, secondItem) -> {
            double firstItemSimilarity = calculateSimilarity(firstItem.getName(), filter);
            double secondItemSimilarity = calculateSimilarity(secondItem.getName(), filter);
            return (int) Math.signum(secondItemSimilarity - firstItemSimilarity);
        };
    }


    /**
     * Determines String similarity between str1 and str2 on scale from 0.0 to 1.0
     * @param str1 String 1
     * @param str2 String 2
     * @return Double between 0.0 and 1.0 that reflects string similarity
     */
    private static double calculateSimilarity(String str1, String str2) {
        int longerLength = Math.max(str1.length(), str2.length());

        if (longerLength == 0) return 1.0;

        int distanceBetweenStrings = levenshteinDistance(str1, str2);
        return (longerLength - distanceBetweenStrings) / (double) longerLength;
    }

    /**
     * Levershtein distance algorithm
     * https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     *
     * @param str1 String 1
     * @param str2 String 2
     * @return Number of characters the strings differ by
     */
    private static int levenshteinDistance(String str1, String str2) {
        if (str1.equals(str2)) return 0;
        if (str1.length() == 0) return str2.length();
        if (str2.length() == 0) return str1.length();

        int[] cost = new int[str1.length() + 1];
        int[] newcost = new int[str1.length() + 1];

        // initial cost of skipping prefix in str1
        for (int i = 0; i < cost.length; i++) cost[i] = i;

        // transformation cost for each letter in str2
        for (int j = 1; j <= str2.length(); j++) {
            // initial cost of skipping prefix in String str2
            newcost[0] = j;

            // transformation cost for each letter in str1
            for(int i = 1; i < cost.length; i++) {
                // matching current letters in both strings
                int match = (str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert  = cost[i] + 1;
                int cost_delete  = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
            }

            int[] tmp = cost;
            cost = newcost;
            newcost = tmp;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[str1.length()];
    }
}
