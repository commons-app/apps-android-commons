package fr.free.nrw.commons.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringSortingUtilsTest {

    @Test
    public void testSortingNumbersBySimilarity() throws Exception {
        List<String> actualList = Arrays.asList("1234567", "4567", "12345", "123", "1234");
        List<String> expectedList = Arrays.asList("1234", "12345", "123", "1234567", "4567");

        Collections.sort(actualList, StringSortingUtils.sortBySimilarity("1234"));
        Assert.assertEquals(expectedList, actualList);
    }

    @Test
    public void testSortingTextBySimilarity() throws Exception {
        List<String> actualList = Arrays.asList("The quick brown fox",
                "quick brown fox",
                "The",
                "The quick ",
                "The fox",
                "brown fox",
                "fox");
        List<String> expectedList = Arrays.asList("The",
                "The fox",
                "The quick ",
                "The quick brown fox",
                "quick brown fox",
                "brown fox",
                "fox");

        Collections.sort(actualList, StringSortingUtils.sortBySimilarity("The"));
        Assert.assertEquals(expectedList, actualList);
    }
}