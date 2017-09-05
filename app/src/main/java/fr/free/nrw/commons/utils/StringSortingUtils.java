package fr.free.nrw.commons.utils;

import info.debatty.java.stringsimilarity.Levenshtein;
import java.util.Comparator;

public class StringSortingUtils {

    private StringSortingUtils() {
        //no-op
    }

    public static Comparator<String> sortBySimilarity(final String filter) {
        return (firstItem, secondItem) -> {
            double firstItemSimilarity = StringSortingUtils.calculateSimilarity(firstItem, filter);
            double secondItemSimilarity = StringSortingUtils.calculateSimilarity(secondItem, filter);
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