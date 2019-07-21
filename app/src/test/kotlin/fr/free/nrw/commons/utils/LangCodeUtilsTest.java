package fr.free.nrw.commons.utils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class LangCodeUtilsTest {
    @Test
    public void testFixLanguageCodeIw(){
        assertTrue("Expected 'he' as result", LangCodeUtils.fixLanguageCode("iw").contentEquals("he"));
    }
    @Test
    public void testFixLanguageCodeIn(){
        assertTrue("Expected 'id' as result", LangCodeUtils.fixLanguageCode("in").contentEquals("id"));
    }
    @Test
    public void testFixLanguageCodeJi(){
        assertTrue("Expected 'yi' as result", LangCodeUtils.fixLanguageCode("ji").contentEquals("yi"));
    }
    @Test
    public void testFixLanguageCodeDefault(){
        assertTrue("Expected 'en' as result", LangCodeUtils.fixLanguageCode("en").contentEquals("en"));
    }
}
