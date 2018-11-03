package fr.free.nrw.commons.utils;

import java.util.Comparator;
import java.util.Locale;

import info.debatty.java.stringsimilarity.Levenshtein;

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
    public static Comparator<String> sortBySimilarity(final String filter) {
        return (firstItem, secondItem) -> {
            double firstItemSimilarity = calculateSimilarity(firstItem, filter);
            double secondItemSimilarity = calculateSimilarity(secondItem, filter);
            return (int) Math.signum(secondItemSimilarity - firstItemSimilarity);
        };
    }


    /**
     * Determines String similarity between str1 and str2 on scale from 0.0 to 1.0
     * Uses the Levenshtein algorithm.
     * @param str1 String 1
     * @param str2 String 2
     * @return Double between 0.0 and 1.0 that reflects string similarity
     */
    private static double calculateSimilarity(String str1, String str2) {
        int longerLength = Math.max(str1.length(), str2.length());

        if (longerLength == 0) {
            return 1.0;
        }

        double distanceBetweenStrings = new Levenshtein().distance(str1, str2);
        return (longerLength - distanceBetweenStrings) / (double) longerLength;
    }
}
