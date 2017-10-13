package fr.free.nrw.commons.utils;

import java.util.Comparator;

import info.debatty.java.stringsimilarity.Levenshtein;

public class StringSortingUtils {

    private StringSortingUtils() {
        //no-op
    }

    /**
     * Returns Comparator for sorting strings by its similarity with Levenshtein
     * algorithm. By using this Comparator we get results from the highest to
     * the lowest match.
     *
     * @param filter pattern to compare similarity
     * @return Comparator with string similarity
     */

    public static Comparator<String> sortBySimilarity(final String filter) {
        return (firstItem, secondItem) -> {
            double firstItemSimilarity = calculateSimilarity(firstItem, filter);
            double secondItemSimilarity = calculateSimilarity(secondItem, filter);
            return (int) Math.signum(secondItemSimilarity - firstItemSimilarity);
        };
    }

    private static double calculateSimilarity(String firstString, String secondString) {
        String longer = firstString.toLowerCase();
        String shorter = secondString.toLowerCase();

        if (firstString.length() < secondString.length()) {
            longer = secondString;
            shorter = firstString;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
        }

        double distanceBetweenStrings = new Levenshtein().distance(longer, shorter);
        return (longerLength - distanceBetweenStrings) / (double) longerLength;
    }
}