package fr.free.nrw.commons.upload

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadFBMDTest {
    @Test
    fun isFacebookMetadataWhenFbmdFollowsPsBlockClosely() {
        assertTrue(ReadFBMD.isFacebookMetadata(10, 11))
        assertTrue(ReadFBMD.isFacebookMetadata(10, 10 + 0x7F))
    }

    @Test
    fun isNotFacebookMetadataWhenMarkersAreMissing() {
        assertFalse(ReadFBMD.isFacebookMetadata(-1, 20))
        assertFalse(ReadFBMD.isFacebookMetadata(10, -1))
        assertFalse(ReadFBMD.isFacebookMetadata(0, 20))
    }

    @Test
    fun isNotFacebookMetadataWhenFbmdIsNotAfterPsBlock() {
        assertFalse(ReadFBMD.isFacebookMetadata(10, 10))
        assertFalse(ReadFBMD.isFacebookMetadata(20, 10))
    }

    @Test
    fun isNotFacebookMetadataWhenFbmdIsTooFarFromPsBlock() {
        assertFalse(ReadFBMD.isFacebookMetadata(10, 10 + 0x80))
    }
}
