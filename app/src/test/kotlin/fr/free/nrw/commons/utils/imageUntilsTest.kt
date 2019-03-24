package fr.free.nrw.commons.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class imagecheck {
    @Test
    fun validateImageForOkImage() {
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/checkalgo1.png"))
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/checkalgo2.png"))
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/checkalgo3.png"))
        assertEquals(ImageUtils.IMAGE_OK, ImageUtils.checkIfImageIsTooDark("src/test/resources/fr/free/nrw/commons/utils/checkalgo4.png"))
    }
}
