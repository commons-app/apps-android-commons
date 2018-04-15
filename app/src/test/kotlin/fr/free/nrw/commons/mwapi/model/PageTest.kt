package fr.free.nrw.commons.mwapi.model

import org.junit.Assert.assertNotNull
import org.junit.Test

class PageTest {
    @Test
    fun categoriesDefaultToSafeValue() {
        val page = Page()
        assertNotNull(page.getCategories())
    }
}