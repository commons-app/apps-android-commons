package fr.free.nrw.commons.utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class StringUtilsTest {
    @Test
    public void testFixLanguageCodeIw(){
        assertTrue("Expected 'he' as result", StringUtils.fixLanguageCode("iw").contentEquals("he"));
    }
    @Test
    public void testFixLanguageCodeIn(){
        assertTrue("Expected 'id' as result", StringUtils.fixLanguageCode("in").contentEquals("id"));
    }
    @Test
    public void testFixLanguageCodeJi(){
        assertTrue("Expected 'yi' as result", StringUtils.fixLanguageCode("ji").contentEquals("yi"));
    }
    @Test
    public void testFixLanguageCodeDefault(){
        assertTrue("Expected 'en' as result", StringUtils.fixLanguageCode("en").contentEquals("en"));
    }
}
