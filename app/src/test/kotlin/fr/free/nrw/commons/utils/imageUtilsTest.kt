package fr.free.nrw.commons.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class imagecheck {
    @Test
    fun validateImageForOkImage() {
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/check1.jpg"))
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/check2.jpg"))
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/check3.jpg"))
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/check4.jpg"))
        assertEquals(ImageUtils.IMAGE_DARK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/check5.jpg"))
    }
}
