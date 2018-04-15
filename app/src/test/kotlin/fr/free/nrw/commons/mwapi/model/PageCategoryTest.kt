package fr.free.nrw.commons.mwapi.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PageCategoryTest {
    @Test
    fun stripPrefix_whenPresent() {
        val testObject = PageCategory()
        testObject.title = "Category:Foo"
        assertEquals("Foo", testObject.withoutPrefix())
    }

    @Test
    fun stripPrefix_prefixAbsent() {
        val testObject = PageCategory()
        testObject.title = "Foo_Bar"
        assertEquals("Foo_Bar", testObject.withoutPrefix())
    }
}