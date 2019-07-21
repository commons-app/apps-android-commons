package fr.free.nrw.commons.utils;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MediaDataExtractorUtilTest {

    @Test
    public void extractCategoriesFromList() {
        List<String> strings = MediaDataExtractorUtil.extractCategoriesFromList("Watercraft 2018|Watercraft|2018");
        assertEquals(strings.size(), 3);
    }

    @Test
    public void extractCategoriesFromEmptyList() {
        List<String> strings = MediaDataExtractorUtil.extractCategoriesFromList("");
        assertEquals(strings.size(), 0);
    }

    @Test
    public void extractCategoriesFromNullList() {
        List<String> strings = MediaDataExtractorUtil.extractCategoriesFromList(null);
        assertEquals(strings.size(), 0);
    }

    @Test
    public void extractCategoriesFromListWithEmptyValues() {
        List<String> strings = MediaDataExtractorUtil.extractCategoriesFromList("Watercraft 2018||");
        assertEquals(strings.size(), 1);
    }

    @Test
    public void extractCategoriesFromListWithWhitespaces() {
        List<String> strings = MediaDataExtractorUtil.extractCategoriesFromList("Watercraft 2018| | ||");
        assertEquals(strings.size(), 1);
    }
}